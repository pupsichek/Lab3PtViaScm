package com.example.pt.lab3.configuration;

import com.example.pt.lab3.pojo.session.SessionInformation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.context.annotation.SessionScope;

import java.util.Collections;

@Configuration
@Slf4j
public class GameConfiguration {
    public static final String OPTIMISTIC_RETRY = "retryTemplateOptimisticLock";

    @Bean(value = OPTIMISTIC_RETRY)
    @Primary
    public RetryTemplate retryTemplateOptimisticLock() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3,
                Collections.singletonMap(OptimisticLockingFailureException.class, true));
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }

    @Bean
    @SessionScope
    public SessionInformation sessionInformation() {
        return new SessionInformation();
    }
}
