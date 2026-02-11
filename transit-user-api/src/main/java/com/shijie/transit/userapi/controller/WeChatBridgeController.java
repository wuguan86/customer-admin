package com.shijie.transit.userapi.controller;

import com.shijie.transit.userapi.wechat.WeChatBridgeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 处理来自 Python Sidecar 的微信消息桥接请求
 */
@RestController
@RequestMapping("/api/wechat")
public class WeChatBridgeController {
    private static final Logger log = LoggerFactory.getLogger(WeChatBridgeController.class);

    @PostMapping("/receive")
    public void receive(@RequestBody WeChatBridgeMessage message) {
        // 暂时只打印日志，后续可在此处接入 Dify 或其他逻辑
        log.info("收到微信消息: contact={} content={} isSelf={}",
                message.getContact(), message.getContent(), message.getIsSelf());
    }
}