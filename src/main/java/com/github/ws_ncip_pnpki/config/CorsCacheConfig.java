package com.github.ws_ncip_pnpki.config;

import com.github.ws_ncip_pnpki.service.ExternalSystemService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableCaching
@EnableScheduling
public class CorsCacheConfig {

    @Bean
    @Cacheable(value = "corsOrigins", unless = "#result.isEmpty()")
    public List<String> getAllowedOrigins(ExternalSystemService externalSystemService) {
        return externalSystemService.getAllActiveExternalSystemUrls();
    }

    // Clear cache every 5 minutes to refresh dynamic origins
    @Scheduled(fixedRate = 300000)
    public void clearCorsCache() {
        // Cache eviction logic if needed
    }
}
