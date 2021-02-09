package com.symphony.ps.pollbot.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class HealthApi {

    @RequestMapping("/healthz")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "UP");
    }
}
