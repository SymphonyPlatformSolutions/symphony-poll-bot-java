package com.symphony.ps.pollbot.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {
    @GetMapping("/healthz")
    public String getHealth() {
        return "OK";
    }
}
