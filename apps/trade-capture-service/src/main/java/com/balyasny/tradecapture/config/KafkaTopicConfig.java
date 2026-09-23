package com.balyasny.tradecapture.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declarative topic provisioning. In production these topics are managed by
 * Terraform/Confluent Control Center; NewTopic beans here keep local/dev/CI
 * environments self-contained (KafkaAdmin auto-creates on startup).
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${trade-capture.topics.executions}")
    private String executionsTopic;

    @Value("${trade-capture.topics.enriched}")
    private String enrichedTopic;

    @Value("${trade-capture.topics.dlq}")
    private String dlqTopic;

    @Bean
    public NewTopic executionsTopic() {
        return TopicBuilder.name(executionsTopic).partitions(6).replicas(1)
                .config("retention.ms", "604800000") // 7 days
                .config("cleanup.policy", "delete")
                .build();
    }

    @Bean
    public NewTopic enrichedTopic() {
        return TopicBuilder.name(enrichedTopic).partitions(6).replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(dlqTopic).partitions(3).replicas(1)
                .config("retention.ms", "2592000000") // 30 days for forensics
                .build();
    }
}
