package com.task.converter.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic requestedTopic(@Value("${app.kafka.topics.input}") String name) {
        return TopicBuilder.name(name)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic completedTopic(@Value("${app.kafka.topics.output}") String name) {
        return TopicBuilder.name(name)
                .partitions(2)
                .replicas(1)
                .build();
    }

}
