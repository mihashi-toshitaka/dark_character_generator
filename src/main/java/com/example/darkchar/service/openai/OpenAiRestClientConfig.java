package com.example.darkchar.service.openai;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiRestClientConfig {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    static final String BASE_URL = "https://api.openai.com/v1";

    @Bean
    @Qualifier("openAiClientHttpRequestFactory")
    ClientHttpRequestFactory openAiClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    @Bean
    @Qualifier("openAiRestClientBuilder")
    RestClient.Builder openAiRestClientBuilder(RestClient.Builder builder,
            @Qualifier("openAiClientHttpRequestFactory") ClientHttpRequestFactory requestFactory) {
        return builder.clone()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory);
    }
}
