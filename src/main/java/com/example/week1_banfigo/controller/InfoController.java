package com.example.week1_banfigo.controller;

import java.util.Map;
import java.util.HashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {
    @GetMapping("/api/info")
    public Map<String, String> getInfo(){
        Map<String, String> response=new HashMap<>();
        response.put("ProjectName","Week-1 Assessment");
        response.put("Date","Today");
        response.put("Version","1.0.0");

        return response;
    }
}
