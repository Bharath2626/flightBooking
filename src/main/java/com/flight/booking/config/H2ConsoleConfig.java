package com.flight.booking.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Registers the H2 web console at /h2-console (dev only, active under the 'h2' profile).
 * Registered explicitly because the Spring Boot auto-config for it isn't picking up here.
 */
@Configuration
@Profile("h2")
public class H2ConsoleConfig {

    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet() {
        ServletRegistrationBean<JakartaWebServlet> registration =
                new ServletRegistrationBean<>(new JakartaWebServlet(), "/h2-console/*");
        registration.addInitParameter("webAllowOthers", "true");
        registration.addInitParameter("trace", "");
        registration.setName("H2Console");
        return registration;
    }
}
