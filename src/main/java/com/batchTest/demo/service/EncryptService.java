package com.batchTest.demo.service;

import com.batchTest.demo.client.KSignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EncryptService {

    private final KSignClient kSignClient;

    public String encrypt(String value) {

        if (value == null || value.isBlank()) return value;

        // 이미 암호화된 데이터 skip
        if (value.startsWith("ENC(")) return value;

        return kSignClient.encrypt(value);
    }
}