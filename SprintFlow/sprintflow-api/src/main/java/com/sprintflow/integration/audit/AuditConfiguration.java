package com.sprintflow.integration.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(AuditProperties.class)
public class AuditConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "sprintflow.audit.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    AuditClient auditClient(RestClient.Builder builder, AuditProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        builder.requestFactory(requestFactory);
        return new HttpAuditClient(builder, properties);
    }

    @Bean(name = "auditTaskExecutor", defaultCandidate = false)
    @ConditionalOnProperty(
            name = "sprintflow.audit.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(250);
        executor.setThreadNamePrefix("audit-forwarder-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }
}
