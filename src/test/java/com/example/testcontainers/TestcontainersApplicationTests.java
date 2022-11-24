package com.example.testcontainers;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import com.example.testcontainers.kafka.KafkaListener;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TestcontainersApplicationTests {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));

    //@Container
    private static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq").withTag("3-management-alpine"));

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @BeforeAll
    static void initContainers() {
//        kafkaContainer = new KafkaContainer(
//                DockerImageName.parse("confluentinc/cp-kafka"));
//        kafkaContainer.start();
//
//        String bootstrapServers = kafkaContainer.getBootstrapServers();
//        System.out.println("Bootstrap servers " + bootstrapServers);
//
//        System.setProperty("app.kafka.bootstrapAddress", bootstrapServers);
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {
        //registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        //registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);

        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void rabbitmqTest() {

    }

    @Test
    void kafkaTest() {
        kafkaTemplate.send("tests", "Hello, World!");
        kafkaTemplate.send("tests", "First Message");
        kafkaTemplate.send("tests", "Second Message");
        kafkaTemplate.send("tests", "Third Message");

        //Assertions.assertEquals(4, KafkaListener.messages.size());
        await().atMost(5, TimeUnit.SECONDS).until(() -> KafkaListener.messages.size(), is(4));
    }

    private Callable<Boolean> isMessageConsumed() {
        return () -> kafkaTemplate.receive("tests", 1, 1).value().contains("Hello, World!");
    }
}
