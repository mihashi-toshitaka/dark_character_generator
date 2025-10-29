package com.example.darkchar.service.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

@SpringJUnitConfig(classes = { OpenAiRestClientConfig.class, OpenAiRestClientConfigTest.TestConfig.class })
class OpenAiRestClientConfigTest {

    @Configuration
    static class TestConfig {
        @Bean
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }

    @Autowired
    @Qualifier("openAiClientHttpRequestFactory")
    ClientHttpRequestFactory requestFactory;

    @Autowired
    @Qualifier("openAiRestClientBuilder")
    RestClient.Builder builder;

    @Test
    void clientHttpRequestFactoryUsesConfiguredTimeouts() {
        assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
        SimpleClientHttpRequestFactory simpleFactory = (SimpleClientHttpRequestFactory) requestFactory;
        assertThat(simpleFactory.getConnectTimeout()).isEqualTo(OpenAiRestClientConfig.CONNECT_TIMEOUT);
        assertThat(simpleFactory.getReadTimeout()).isEqualTo(OpenAiRestClientConfig.READ_TIMEOUT);
    }

    @Test
    void restClientBuilderUsesConfiguredFactory() {
        RestClient restClient = builder.build();
        Object actualFactory = ReflectionTestUtils.getField(restClient, "clientHttpRequestFactory");
        assertThat(actualFactory).isSameAs(requestFactory);
    }
}
