package com.shijie.transit.userapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shijie.transit.common.db.entity.TaskEntity;
import com.shijie.transit.common.tenant.TenantContext;
import com.shijie.transit.userapi.mapper.TaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    private final TaskMapper taskMapper;

    public TaskService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public List<TaskEntity> list(Long userId) {
        LambdaQueryWrapper<TaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskEntity::getUserId, userId);
        wrapper.orderByDesc(TaskEntity::getCreatedAt);
        return taskMapper.selectList(wrapper);
    }

    @Transactional
    public TaskEntity create(Long userId, TaskEntity entity) {
        entity.setUserId(userId);
        entity.setTenantId(TenantContext.getTenantId());
        entity.setStatus("PENDING");
        entity.setType(entity.getType() == null ? "DEFAULT" : entity.getType());
        taskMapper.insert(entity);
        return entity;
    }

    @Transactional
    public TaskEntity update(Long userId, Long id, TaskEntity entity) {
        TaskEntity existing = taskMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new RuntimeException("Task not found or permission denied");
        }
        // Only allow updating editable fields
        if (entity.getName() != null) existing.setName(entity.getName());
        if (entity.getContent() != null) existing.setContent(entity.getContent());
        if (entity.getStatus() != null) existing.setStatus(entity.getStatus());
        if (entity.getType() != null) existing.setType(entity.getType());
        if (entity.getPromptTemplateId() != null) existing.setPromptTemplateId(entity.getPromptTemplateId());
        if (entity.getKnowledgeBaseId() != null) existing.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        
        taskMapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        TaskEntity existing = taskMapper.selectById(id);
        if (existing != null && existing.getUserId().equals(userId)) {
            taskMapper.deleteById(id);
        }
    }
}
