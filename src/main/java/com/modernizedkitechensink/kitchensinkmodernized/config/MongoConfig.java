package com.modernizedkitechensink.kitchensinkmodernized.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * MongoDB & Internationalization Configuration.
 * 
 * Enables:
 * 1. MongoDB auditing for automatic population of audit fields
 * 2. Internationalization (i18n) support for multi-language messages
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
  
  /**
   * Configure LocaleResolver to use Accept-Language header.
   * Supports: English (en), Hindi (hi), Spanish (es)
   */
  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(Locale.US);  // Default to English
    resolver.setSupportedLocales(Arrays.asList(
      Locale.US,           // English
      new Locale("hi"),    // Hindi
      new Locale("es")     // Spanish
    ));
    return resolver;
  }
  
  /**
   * Configure MessageSource for i18n message bundles.
   * Reads from messages.properties, messages_hi.properties, messages_es.properties
   */
  @Bean
  public MessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setUseCodeAsDefaultMessage(true);  // Return code if message not found
    return messageSource;
  }
}

