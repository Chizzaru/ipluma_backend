package com.github.ws_ncip_pnpki.config;


import com.github.ws_ncip_pnpki.service.ExternalSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ExternalSystemService externalSystemService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        List<String> staticOrigins = Arrays.asList(
                "http://pluma.local:8006",
                "http://localhost:5173",
                "http://192.168.5.117:8006",
                "http://172.27.80.1:5173",
                "http://172.17.5.70:5173",
                "https://ipluma.ncip.gov.ph/"
        );

        // Get dynamic origins from database (cached)
        List<String> dynamicOrigins = externalSystemService.getAllActiveExternalSystemUrls();

        // Combine and deduplicate all origins
        List<String> allOrigins = Stream.concat(staticOrigins.stream(), dynamicOrigins.stream())
                .distinct()
                .toList();

        registry.addEndpoint("/ws")
                .setAllowedOrigins(allOrigins.toArray(new String[0]));
                //.withSockJS();
    }

}
