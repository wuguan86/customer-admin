package com.shijie.transit.userapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shijie.transit.common.db.entity.MarketingCommentConfigEntity;
import com.shijie.transit.common.db.entity.MarketingLikeConfigEntity;
import com.shijie.transit.common.db.entity.MarketingScheduledTaskEntity;
import com.shijie.transit.userapi.mapper.MarketingCommentConfigMapper;
import com.shijie.transit.userapi.mapper.MarketingLikeConfigMapper;
import com.shijie.transit.userapi.mapper.MarketingScheduledTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MarketingService {
    private final MarketingLikeConfigMapper likeConfigMapper;
    private final MarketingCommentConfigMapper commentConfigMapper;
    private final MarketingScheduledTaskMapper scheduledTaskMapper;

    public MarketingService(MarketingLikeConfigMapper likeConfigMapper,
                            MarketingCommentConfigMapper commentConfigMapper,
                            MarketingScheduledTaskMapper scheduledTaskMapper) {
        this.likeConfigMapper = likeConfigMapper;
        this.commentConfigMapper = commentConfigMapper;
        this.scheduledTaskMapper = scheduledTaskMapper;
    }

    public MarketingLikeConfigEntity getLikeConfig(Long userId) {
        LambdaQueryWrapper<MarketingLikeConfigEntity> query = new LambdaQueryWrapper<>();
        query.eq(MarketingLikeConfigEntity::getUserId, userId);
        return likeConfigMapper.selectOne(query);
    }

    @Transactional
    public MarketingLikeConfigEntity saveLikeConfig(Long userId, MarketingLikeConfigEntity config) {
        MarketingLikeConfigEntity existing = getLikeConfig(userId);
        if (existing == null) {
            config.setUserId(userId);
            likeConfigMapper.insert(config);
            return config;
        } else {
            config.setId(existing.getId());
            config.setUserId(userId);
            likeConfigMapper.updateById(config);
            return config;
        }
    }

    public MarketingCommentConfigEntity getCommentConfig(Long userId) {
        LambdaQueryWrapper<MarketingCommentConfigEntity> query = new LambdaQueryWrapper<>();
        query.eq(MarketingCommentConfigEntity::getUserId, userId);
        return commentConfigMapper.selectOne(query);
    }

    @Transactional
    public MarketingCommentConfigEntity saveCommentConfig(Long userId, MarketingCommentConfigEntity config) {
        MarketingCommentConfigEntity existing = getCommentConfig(userId);
        if (existing == null) {
            config.setUserId(userId);
            commentConfigMapper.insert(config);
            return config;
        } else {
            config.setId(existing.getId());
            config.setUserId(userId);
            commentConfigMapper.updateById(config);
            return config;
        }
    }

    public List<MarketingScheduledTaskEntity> getScheduledTasks(Long userId, String taskType) {
        LambdaQueryWrapper<MarketingScheduledTaskEntity> query = new LambdaQueryWrapper<>();
        query.eq(MarketingScheduledTaskEntity::getUserId, userId);
        if (taskType != null && !taskType.isEmpty()) {
            query.eq(MarketingScheduledTaskEntity::getTaskType, taskType);
        }
        query.orderByDesc(MarketingScheduledTaskEntity::getCreatedAt);
        return scheduledTaskMapper.selectList(query);
    }

    @Transactional
    public MarketingScheduledTaskEntity saveScheduledTask(Long userId, MarketingScheduledTaskEntity task) {
        task.setUserId(userId);
        if (task.getId() == null) {
            scheduledTaskMapper.insert(task);
        } else {
            // Ensure user owns the task
            MarketingScheduledTaskEntity existing = scheduledTaskMapper.selectById(task.getId());
            if (existing != null && existing.getUserId().equals(userId)) {
                scheduledTaskMapper.updateById(task);
            } else {
                throw new RuntimeException("Task not found or permission denied");
            }
        }
        return task;
    }

    @Transactional
    public void deleteScheduledTask(Long userId, Long taskId) {
        LambdaQueryWrapper<MarketingScheduledTaskEntity> query = new LambdaQueryWrapper<>();
        query.eq(MarketingScheduledTaskEntity::getId, taskId);
        query.eq(MarketingScheduledTaskEntity::getUserId, userId);
        scheduledTaskMapper.delete(query);
    }
}
