package com.modernizedkitechensink.kitchensinkmodernized.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * MongoDB Configuration.
 * 
 * Enables MongoDB auditing for automatic population of:
 * - @CreatedDate fields (e.g., Member.createdAt)
 * - @LastModifiedDate fields (e.g., Member.updatedAt)
 * - @CreatedBy fields (e.g., Member.createdBy)
 * - @LastModifiedBy fields (e.g., Member.updatedBy)
 * 
 * @author Nitin Agarwal
 * @since 1.0.0
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
  
  /**
   * Provides the current auditor (logged-in user) for Spring Data auditing.
   * This enables @CreatedBy and @LastModifiedBy annotations to work automatically.
   * 
   * @return AuditorAware implementation that returns the current username
   */
  @Bean
  public AuditorAware<String> auditorProvider() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      
      if (authentication == null || !authentication.isAuthenticated() || 
          "anonymousUser".equals(authentication.getPrincipal())) {
        return Optional.of("system");
      }
      
      return Optional.of(authentication.getName());
    };
  }
}

