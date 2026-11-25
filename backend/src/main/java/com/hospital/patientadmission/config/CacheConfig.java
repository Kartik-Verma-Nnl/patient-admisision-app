package com.hospital.patientadmission.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    /**
     * If Redis is down or a cached value can't be read, log it and fall through to the database
     * instead of failing the request. Redis is an optimisation, not a dependency for correctness.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(false);
    }

    /** Apply cache evictions only after the DB transaction commits, so readers never re-cache stale data. */
    @Bean
    public RedisCacheManagerBuilderCustomizer transactionAwareCaches() {
        return builder -> builder.transactionAware();
    }
}
