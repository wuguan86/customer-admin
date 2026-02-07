package com.shijie.transit.userapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName TestController
 * @Description
 * @Author 你的名字
 * @Date 2026/2/6 17:31
 * @Version 1.0
 */
@RestController
public class TestController {
    @GetMapping("/hello")
    public String test(){
        return "hello";
    }
}
