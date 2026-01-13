package com.banking.platform.customers.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String CUSTOMER_TOPIC = "banking.customers.v1";

    @Bean
    public NewTopic customerTopic() {
        return TopicBuilder.name(CUSTOMER_TOPIC)
                .partitions(1) // Para pruebas locales 1 es suficiente
                .replicas(1)
                .build();
    }
}