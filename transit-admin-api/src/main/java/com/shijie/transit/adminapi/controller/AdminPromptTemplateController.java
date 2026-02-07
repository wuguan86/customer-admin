package com.shijie.transit.adminapi.controller;

import com.shijie.transit.adminapi.service.PromptTemplateService;
import com.shijie.transit.common.db.entity.PromptTemplateEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提示词模板管理接口
 */
@RestController
@RequestMapping("/api/admin/prompt-templates")
public class AdminPromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    public AdminPromptTemplateController(PromptTemplateService promptTemplateService) {
        this.promptTemplateService = promptTemplateService;
    }

    @GetMapping
    public List<PromptTemplateEntity> list() {
        return promptTemplateService.list();
    }

    @PostMapping
    public PromptTemplateEntity create(@RequestBody PromptTemplateEntity entity) {
        return promptTemplateService.create(entity);
    }

    @PutMapping("/{id}")
    public PromptTemplateEntity update(@PathVariable("id") Long id, @RequestBody PromptTemplateEntity entity) {
        return promptTemplateService.update(id, entity);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        promptTemplateService.delete(id);
    }

    @GetMapping("/{id}")
    public PromptTemplateEntity getById(@PathVariable("id") Long id) {
        return promptTemplateService.getById(id);
    }
}
