package com.lloyds.transaction.config;

import com.lloyds.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class QuartzConfig {

    private final DataSource dataSource; // Spring-managed DataSource

    @Value("${spring.quartz.properties.org.quartz.scheduler.instanceName}")
    private String instanceName;

    @Value("${spring.quartz.properties.org.quartz.scheduler.instanceId}")
    private String instanceId;

    @Value("${spring.quartz.properties.org.quartz.jobStore.driverDelegateClass}")
    private String jobStoreDriverDelegateClass;

    @Value("${spring.quartz.properties.org.quartz.jobStore.tablePrefix}")
    private String jobStoreTablePrefix;

    @Value("${spring.quartz.properties.org.quartz.jobStore.isClustered}")
    private String jobStoreIsClustered;

    @Value("${spring.quartz.properties.org.quartz.threadPool.class}")
    private String threadPoolClass;

    @Value("${spring.quartz.properties.org.quartz.threadPool.threadCount}")
    private String threadPoolThreadCount;

    @Value("${spring.quartz.properties.org.quartz.jobStore.dataSource}")
    private String jobStoreDataSource;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(TransactionService transactionService) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();

        // Use the Spring-managed DataSource
        factory.setDataSource(dataSource);
        factory.setSchedulerContextAsMap(Map.of("transactionService", transactionService));

//        // Quartz scheduler properties
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", instanceName);
        props.setProperty("org.quartz.scheduler.instanceId", instanceId);
        props.setProperty("org.quartz.jobStore.driverDelegateClass", jobStoreDriverDelegateClass);
        props.setProperty("org.quartz.jobStore.tablePrefix", jobStoreTablePrefix);
        props.setProperty("org.quartz.jobStore.isClustered", jobStoreIsClustered);
        props.setProperty("org.quartz.threadPool.class", threadPoolClass);
        props.setProperty("org.quartz.threadPool.threadCount", threadPoolThreadCount);
        props.setProperty("org.quartz.jobStore.dataSource", jobStoreDataSource);

        factory.setQuartzProperties(props);
        return factory;
    }
}