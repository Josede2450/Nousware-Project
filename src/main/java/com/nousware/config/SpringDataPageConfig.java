package com.nousware.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Enables stable JSON serialization for Spring Data Page objects.
 * Removes "Serializing PageImpl instances as-is" warning.
 */
@Configuration
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class SpringDataPageConfig {
    // nothing else required
}
