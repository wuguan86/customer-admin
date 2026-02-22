package com.shijie.transit.userapi.controller;

import com.shijie.transit.common.db.entity.TaskEntity;
import com.shijie.transit.common.security.TransitPrincipal;
import com.shijie.transit.common.web.Result;
import com.shijie.transit.userapi.service.TaskService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/tasks")
public class UserTaskController {
    private final TaskService taskService;

    public UserTaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TransitPrincipal principal = (TransitPrincipal) authentication.getPrincipal();
        return principal.subjectId();
    }

    @GetMapping
    public Result<List<TaskEntity>> list() {
        return Result.success(taskService.list(currentUserId()));
    }

    @PostMapping
    public Result<TaskEntity> create(@RequestBody TaskEntity entity) {
        return Result.success(taskService.create(currentUserId(), entity));
    }

    @PutMapping("/{id}")
    public Result<TaskEntity> update(@PathVariable("id") Long id, @RequestBody TaskEntity entity) {
        return Result.success(taskService.update(currentUserId(), id, entity));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        taskService.delete(currentUserId(), id);
        return Result.success(null);
    }
}
