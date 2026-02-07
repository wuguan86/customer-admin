package com.shijie.transit.userapi.controller;

import com.shijie.transit.common.db.entity.TaskEntity;
import com.shijie.transit.common.security.TransitPrincipal;
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
    public List<TaskEntity> list() {
        return taskService.list(currentUserId());
    }

    @PostMapping
    public TaskEntity create(@RequestBody TaskEntity entity) {
        return taskService.create(currentUserId(), entity);
    }

    @PutMapping("/{id}")
    public TaskEntity update(@PathVariable("id") Long id, @RequestBody TaskEntity entity) {
        return taskService.update(currentUserId(), id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        taskService.delete(currentUserId(), id);
    }
}
