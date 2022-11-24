package com.example.testcontainers.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@Service
@Slf4j
public class KafkaListener {

    public static List<String> messages = new ArrayList<>();

    @org.springframework.kafka.annotation.KafkaListener(topics = "tests", groupId = "group_id")
    public static void listenGroupFoo(String message) throws IOException {
        messages.add(message);

        log.info("Received message in group Test with topic test: " + message);
    }

}
