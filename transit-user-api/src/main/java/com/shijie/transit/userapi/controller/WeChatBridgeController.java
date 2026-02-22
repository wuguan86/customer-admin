package com.shijie.transit.userapi.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shijie.transit.common.db.entity.TaskEntity;
import com.shijie.transit.common.web.Result;
import com.shijie.transit.userapi.dify.DifyClient;
import com.shijie.transit.userapi.mapper.TaskMapper;
import com.shijie.transit.userapi.service.DifyContactConversationMappingService;
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

import java.util.Map;

/**
 * 处理来自 Python Sidecar 的微信消息桥接请求
 */
@RestController
@RequestMapping("/api/wechat")
public class WeChatBridgeController {
    private static final Logger log = LoggerFactory.getLogger(WeChatBridgeController.class);

    private final TaskMapper taskMapper;
    private final DifyClient difyClient;
    private final DifyContactConversationMappingService mappingService;
    private final ObjectMapper objectMapper;
    private final RestClient sidecarClient;

    public WeChatBridgeController(TaskMapper taskMapper,
                                  DifyClient difyClient,
                                  DifyContactConversationMappingService mappingService,
                                  ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.difyClient = difyClient;
        this.mappingService = mappingService;
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

        // 2. 查找当前运行中的任务
        // 假设当前只有一个用户在运行，或者这是本地单用户部署
        // 查找 status = 'RUNNING' 的任务
        TaskEntity runningTask = taskMapper.selectOne(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getStatus, "RUNNING")
                .last("limit 1"));

        if (runningTask == null) {
            log.info("未找到运行中的任务，忽略消息");
            return Result.success(null);
        }

        // 3. 处理回复
        try {
            processAndReply(runningTask, message);
        } catch (Exception e) {
            log.error("处理回复失败", e);
        }

        return Result.success(null);
    }

    private void processAndReply(TaskEntity task, WeChatBridgeMessage message) {
        String contact = message.getContact();
        if (!StringUtils.hasText(contact)) {
            log.warn("消息缺少联系人信息，无法回复");
            return;
        }

        // 4. 获取/创建会话 ID
        String conversationId = mappingService.getConversationId(task.getUserId(), task.getId(), contact);

        // 5. 调用 Dify
        // 构造 Dify 请求 Payload
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("query", message.getContent());
        payload.put("user", "user-" + task.getUserId()); // 使用任务归属用户作为 Dify 用户标识

        ObjectNode inputs = payload.putObject("inputs");
        // 如果任务有自定义 Prompt (content)，放入 inputs
        if (StringUtils.hasText(task.getContent())) {
            inputs.put("user_custom_role", task.getContent());
        }

        if (StringUtils.hasText(conversationId)) {
            payload.put("conversation_id", conversationId);
        }

        log.info("调用 Dify API: conversationId={}", conversationId);
        DifyClient.DifyChatResult result = difyClient.chatMessages(payload.toString());

        // 6. 更新会话 ID
        if (StringUtils.hasText(result.conversationId())) {
            mappingService.upsertConversationId(task.getUserId(), task.getId(), contact, result.conversationId());
        }

        String answer = result.answer();
        if (StringUtils.hasText(answer)) {
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