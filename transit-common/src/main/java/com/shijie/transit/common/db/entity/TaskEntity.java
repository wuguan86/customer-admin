package com.shijie.transit.common.db.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户任务表
 */
@TableName("task")
public class TaskEntity extends BaseTenantEntity {

    private Long userId;

    private String name;

    private String content;

    private String status; // PENDING, RUNNING, COMPLETED, FAILED

    private String type; // DEFAULT

    private Long promptTemplateId;

    private String knowledgeBaseId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getPromptTemplateId() {
        return promptTemplateId;
    }

    public void setPromptTemplateId(Long promptTemplateId) {
        this.promptTemplateId = promptTemplateId;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(String knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }
}
