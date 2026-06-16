package com.batchTest.demo.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KSignClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ksign.url}")
    private String ksignUrl;

    public String encrypt(String plainText) {

        Map<String, Object> req = new HashMap<>();
        req.put("data", plainText);

        Map res = restTemplate.postForObject(
                ksignUrl + "/encrypt",
                req,
                Map.class
        );

        return (String) res.get("encData");
    }
}