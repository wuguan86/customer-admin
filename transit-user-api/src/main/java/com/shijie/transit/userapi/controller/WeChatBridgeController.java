package com.shijie.transit.userapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shijie.transit.common.db.entity.RoleEntity;
import com.shijie.transit.common.web.Result;
import com.shijie.transit.userapi.dify.DifyClient;
import com.shijie.transit.userapi.mapper.RoleMapper;
import com.shijie.transit.userapi.service.DifyContactConversationMappingService;
import com.shijie.transit.userapi.service.SessionConfigService;
import com.shijie.transit.userapi.service.SessionHistoryService;
import com.shijie.transit.userapi.wechat.WeChatBridgeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 处理来自 Python Sidecar 的微信消息桥接请求
 */
@RestController
@RequestMapping("/api/wechat")
public class WeChatBridgeController {
    private static final Logger log = LoggerFactory.getLogger(WeChatBridgeController.class);

    private final RoleMapper roleMapper;
    private final DifyClient difyClient;
    private final DifyContactConversationMappingService mappingService;
    private final SessionConfigService sessionConfigService;
    private final SessionHistoryService sessionHistoryService;
    private final ObjectMapper objectMapper;
    private final RestClient sidecarClient;

    public WeChatBridgeController(RoleMapper roleMapper,
                                  DifyClient difyClient,
                                  DifyContactConversationMappingService mappingService,
                                  SessionConfigService sessionConfigService,
                                  SessionHistoryService sessionHistoryService,
                                  ObjectMapper objectMapper) {
        this.roleMapper = roleMapper;
        this.difyClient = difyClient;
        this.mappingService = mappingService;
        this.sessionConfigService = sessionConfigService;
        this.sessionHistoryService = sessionHistoryService;
        this.objectMapper = objectMapper;
        // 默认 sidecar 地址，后续可改为配置注入
        this.sidecarClient = RestClient.builder()
                .baseUrl("http://127.0.0.1:51234")
                .build();
    }

    @PostMapping("/receive")
    public Result<Void> receive(@RequestBody WeChatBridgeMessage message) {
        log.info("收到微信消息: contact={} content={} isSelf={}",
                message.getContact(), message.getContent(), message.getIsSelf());

        // 1. 如果是自己发送的消息，忽略
        if (Boolean.TRUE.equals(message.getIsSelf())) {
            return Result.success(null);
        }

        // 2. 查找当前运行中的角色
        // 假设当前只有一个用户在运行，或者这是本地单用户部署
        // 查找 status = 'RUNNING' 的角色
        RoleEntity runningRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getStatus, "RUNNING")
                .last("limit 1"));

        if (runningRole == null) {
            log.info("未找到运行中的角色，忽略消息");
            return Result.success(null);
        }

        // 3. 处理回复
        try {
            processAndReply(runningRole, message);
        } catch (Exception e) {
            log.error("处理回复失败", e);
        }

        return Result.success(null);
    }

    private void processAndReply(RoleEntity role, WeChatBridgeMessage message) {
        String contact = message.getContact();
        if (!StringUtils.hasText(contact)) {
            log.warn("消息缺少联系人信息，无法回复");
            return;
        }

        // Determine scene type
        String sceneType = "SINGLE";
        if ("GROUP".equalsIgnoreCase(message.getRoomType())) {
            sceneType = "GROUP";
        } else if (StringUtils.hasText(contact) && contact.matches(".*\\(\\d+\\)$")) {
             // Heuristic: if contact ends with (digits), treat as GROUP
             // e.g. "My Group (10)"
             sceneType = "GROUP";
        }

        SessionConfigService.SessionConfigView configView =
                sessionConfigService.getConfig(role.getUserId(), sceneType);

        // 1. Check Enabled
        if (configView == null || configView.sceneConfig() == null ||
                configView.sceneConfig().enabled() == null || configView.sceneConfig().enabled() == 0) {
            log.info("会话配置已禁用 (sceneType={})，忽略消息", sceneType);
            return;
        }

        String content = message.getContent();

        if ("GROUP".equals(sceneType)) {
            // Group Chat Logic

            // 1.1 Time Range
            String startTimeStr = configView.sceneConfig().groupReplyStartTime();
            String endTimeStr = configView.sceneConfig().groupReplyEndTime();
            if (StringUtils.hasText(startTimeStr) && StringUtils.hasText(endTimeStr)) {
                try {
                    LocalTime now = LocalTime.now();
                    LocalTime start = LocalTime.parse(startTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
                    LocalTime end = LocalTime.parse(endTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
                    // Simple range check within a day
                    if (now.isBefore(start) || now.isAfter(end)) {
                        log.info("当前时间 {} 不在群回复时间段 {}-{} 内，忽略消息", now, start, end);
                        return;
                    }
                } catch (Exception e) {
                    log.warn("解析群回复时间段失败: {}-{}", startTimeStr, endTimeStr, e);
                }
            }

            // 1.2 Keyword Trigger
            Integer keywordTriggerEnabled = configView.replyStrategy().groupKeywordTriggerEnabled();
            List<String> triggerKeywords = configView.groupTriggerKeywords();
            if (keywordTriggerEnabled != null && keywordTriggerEnabled == 1) {
                boolean matched = false;
                if (triggerKeywords != null && StringUtils.hasText(content)) {
                    for (String keyword : triggerKeywords) {
                        if (StringUtils.hasText(keyword) && content.contains(keyword)) {
                            matched = true;
                            break;
                        }
                    }
                }
                if (!matched) {
                    log.info("未触发群消息关键词，忽略消息");
                    return;
                }
            }

            // 1.3 Frequency (Cooldown)
            Integer cooldownSec = configView.sceneConfig().groupCooldownSec();
            if (cooldownSec != null && cooldownSec > 0) {
                LocalDateTime lastReply = sessionHistoryService.getLastAiReplyTime(role.getUserId(), role.getId(), "GROUP", contact);
                if (lastReply != null) {
                    Duration duration = Duration.between(lastReply, LocalDateTime.now());
                    if (duration.getSeconds() < cooldownSec) {
                        log.info("群回复频率限制 ({}s < {}s)，忽略消息", duration.getSeconds(), cooldownSec);
                        return;
                    }
                }
            }

        } else {
            // Single Chat Logic

            // 检查是否触发 AI 停止回复
            if (configView.replyStrategy() != null) {
                // 优先检查人工介入 (优先级高于 AI 停止回复)
                Integer manualHandoffEnabled = configView.replyStrategy().manualHandoffEnabled();
                List<String> manualHandoffKeywords = configView.manualHandoffKeywords();

                if (manualHandoffEnabled != null && manualHandoffEnabled == 1 && manualHandoffKeywords != null && StringUtils.hasText(content)) {
                    for (String keyword : manualHandoffKeywords) {
                        if (StringUtils.hasText(keyword) && content.contains(keyword)) {
                            log.info("触发人工介入关键词: {}, 停止 AI 回复并发送提示", keyword);
                            String handoffMsg = configView.replyStrategy().manualHandoffMessage();
                            if (StringUtils.hasText(handoffMsg)) {
                                sendToWeChat(contact, handoffMsg);
                            }
                            return;
                        }
                    }
                }

                Integer stopReplyEnabled = configView.replyStrategy().aiStopReplyEnabled();
                List<String> stopKeywords = configView.aiStopReplyKeywords();

                if (stopReplyEnabled != null && stopReplyEnabled == 1 && stopKeywords != null && StringUtils.hasText(content)) {
                    for (String keyword : stopKeywords) {
                        if (StringUtils.hasText(keyword) && content.contains(keyword)) {
                            log.info("触发 AI 停止回复关键词: {}, 停止回复", keyword);
                            return;
                        }
                    }
                }
            }
        }

        // 4. 获取/创建会话 ID
        String conversationId = mappingService.getConversationId(role.getUserId(), role.getId(), contact);

        // 5. 调用 Dify
        // 构造 Dify 请求 Payload
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("query", message.getContent());
        payload.put("user", "user-" + role.getUserId()); // 使用角色归属用户作为 Dify 用户标识

        ObjectNode inputs = payload.putObject("inputs");
        // 如果角色有自定义 Prompt (content)，放入 inputs
        if (StringUtils.hasText(role.getContent())) {
            inputs.put("user_custom_role", role.getContent());
        }

        int memoryRounds = 5;
        if (configView.sceneConfig().memoryRounds() != null) {
            memoryRounds = Math.max(configView.sceneConfig().memoryRounds(), 1);
        }
        inputs.set("history", objectMapper.valueToTree(
                sessionHistoryService.buildDifyHistory(role.getUserId(), role.getId(), sceneType, contact, memoryRounds)));
        sessionHistoryService.appendMessage(role.getUserId(), role.getId(), sceneType, contact, "USER", message.getContent());

        if (StringUtils.hasText(conversationId)) {
            payload.put("conversation_id", conversationId);
        }

        log.info("调用 Dify API: conversationId={} sceneType={}", conversationId, sceneType);
        DifyClient.DifyChatResult result = difyClient.chatMessages(payload.toString());

        // 6. 更新会话 ID
        if (StringUtils.hasText(result.conversationId())) {
            mappingService.upsertConversationId(role.getUserId(), role.getId(), contact, result.conversationId());
        }

        String answer = result.answer();
        if (StringUtils.hasText(answer)) {
            sessionHistoryService.appendMessage(role.getUserId(), role.getId(), sceneType, contact, "AI", answer);
            log.info("Dify 回复: {}", answer);
            // 7. 发送回复给微信
            sendToWeChat(contact, answer);
        } else {
            log.warn("Dify 返回空回复");
        }
    }

    private void sendToWeChat(String contact, String content) {
        try {
            Map<String, String> body = Map.of(
                    "target", contact,
                    "content", content
            );
            String response = sidecarClient.post()
                    .uri("/command")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            log.info("发送微信回复结果: {}", response);
        } catch (Exception e) {
            log.error("发送微信回复异常", e);
        }
    }
}
