package com.shijie.transit.userapi.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 接收来自 Python Sidecar 的微信消息实体
 */
public class WeChatBridgeMessage {
    private String type;
    private String content;
    private String contact;

    @JsonProperty("is_self")
    private Boolean isSelf;

    private String timestamp;
    private Map<String, Object> meta;

    @JsonProperty("ui_id")
    private String uiId;

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public Boolean getIsSelf() { return isSelf; }
    public void setIsSelf(Boolean isSelf) { this.isSelf = isSelf; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }

    public String getUiId() { return uiId; }
    public void setUiId(String uiId) { this.uiId = uiId; }

    @Override
    public String toString() {
        return "WeChatBridgeMessage{" +
                "type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", contact='" + contact + '\'' +
                ", isSelf=" + isSelf +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}