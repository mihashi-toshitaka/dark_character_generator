package com.example.darkchar.service.openai;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OpenAIのモデル一覧を取得するクライアント。
 */
@Component
public class OpenAiModelCatalogClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiModelCatalogClient.class);
    private static final String BASE_URL = "https://api.openai.com/v1";

    private final RestClient restClient;

    public OpenAiModelCatalogClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    /**
     * 指定されたAPIキーを用いてモデル一覧を取得します。
     *
     * @param apiKey OpenAI APIキー
     * @return モデルIDの一覧
     */
    public List<String> listModels(String apiKey) {
        Objects.requireNonNull(apiKey, "apiKey");
        try {
            logger.info("Fetching OpenAI model catalog");
            ModelListResponse response = restClient.get()
                    .uri("/models")
                    .headers(headers -> {
                        headers.setBearerAuth(apiKey);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .retrieve()
                    .body(ModelListResponse.class);

            if (response == null || response.data() == null) {
                logger.warn("Model catalog response was empty");
                throw new OpenAiIntegrationException("OpenAIモデル一覧を取得できませんでした。");
            }

            List<String> models = response.data().stream()
                    .map(ModelSummary::id)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());

            logger.info("Retrieved {} models from OpenAI", models.size());
            return models;
        } catch (RestClientResponseException ex) {
            logger.warn("OpenAI model catalog request failed: status={} {}", ex.getRawStatusCode(), ex.getStatusText());
            throw new OpenAiIntegrationException("OpenAIモデル一覧の取得に失敗しました。", ex);
        } catch (RestClientException ex) {
            logger.warn("OpenAI model catalog request error: {}", ex.getMessage());
            throw new OpenAiIntegrationException("OpenAIモデル一覧の取得中にエラーが発生しました。", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ModelListResponse(List<ModelSummary> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ModelSummary(String id) {
    }
}
