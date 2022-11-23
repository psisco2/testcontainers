package com.example.testcontainers.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class KafkaListener {

    @org.springframework.kafka.annotation.KafkaListener(topics = "tests", groupId = "Test")
    public void listenGroupFoo(String message) {
        log.info("Received message in group Test with topic test: " + message);
    }

}
