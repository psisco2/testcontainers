package com.example.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;

import com.example.testcontainers.rabbitmq.RabbitMQConfig;
import com.example.testcontainers.rabbitmq.Receiver;
import com.rabbitmq.client.Channel;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TestcontainersApplicationTests {

    static final String topicExchangeName = "spring-boot-exchange";
    static final String queueName = "spring-boot";

    CountDownLatch latch = new CountDownLatch(1);
    List<String> messages = new ArrayList<>();

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));

    @Container
    private static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer(DockerImageName.parse("rabbitmq").withTag("3-management-alpine"))
            .withQueue(queueName)
            .withExchange(topicExchangeName, ExchangeTypes.TOPIC)
            .withBinding(topicExchangeName, queueName, Collections.emptyMap(), "foo.bar.#", "queue")
            .withEnv("spring.rabbitmq.listener.acknowledge-mode", "manual");

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;
    
    @BeforeAll
    static void setUp() {
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
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);

        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void rabbitmqTest() throws InterruptedException {
        //rabbitTemplate.convertAndSend("exchangeIn", "routing.myqueue", "myMessage".getBytes());
        rabbitTemplate.sendAndReceive(topicExchangeName, "foo.bar.baz", new Message("myMessage".getBytes()));

        assertThat(latch.await(10, TimeUnit.SECONDS));

        //await().atMost(5, TimeUnit.SECONDS).until(isRabbitMQMessageReceived(), is("OK"));
    }

    @Test
    void kafkaTest() {
        kafkaTemplate.send("tests", "Hello, World!");
        kafkaTemplate.send("tests", "First Message");
        kafkaTemplate.send("tests", "Second Message");
        kafkaTemplate.send("tests", "Third Message");

        //Assertions.assertEquals(4, KafkaListener.messages.size());
        await().atMost(5, TimeUnit.SECONDS).until(() -> messages.size(), is(4));
    }

    @RabbitListener(queues = queueName)
    void receive(String payload, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        System.out.println("Received message : " + payload);
        channel.basicAck(tag, false);

        latch.countDown();
    }

    @KafkaListener(topics = "tests", groupId = "group_id")
    void listenGroupFoo(String message) throws IOException {
        System.out.println("Received message in group Test with topic test: " + message);

        messages.add(message);
    }

    private Callable<Boolean> isMessageConsumed() {
        return () -> kafkaTemplate.receive("tests", 1, 1).value().contains("Hello, World!");
    }

    private Callable<Object> isRabbitMQMessageReceived() {
        return () -> rabbitTemplate.receiveAndConvert(queueName);
    }
}
