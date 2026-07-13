package com.heichan.camera.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 外部 HTTP 客户端配置。
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient quickRouterRestClient(
            @Value("${ai.quickrouter.connect-timeout-seconds:20}")
            long connectTimeoutSeconds,

            @Value("${ai.quickrouter.read-timeout-seconds:120}")
            long readTimeoutSeconds
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(
                        Duration.ofSeconds(connectTimeoutSeconds)
                )
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                Duration.ofSeconds(readTimeoutSeconds)
        );

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}