package com.example.darkchar.service.openai;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.openai.client.OpenAI;
import com.openai.exceptions.OpenAIException;
import com.openai.models.Model;
import com.openai.models.ModelListResponse;

/**
 * OpenAIのモデル一覧を取得するクライアント。
 */
@Component
public class OpenAiModelCatalogClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiModelCatalogClient.class);
    private static final Pattern GPT_PREFIX = Pattern.compile("^gpt-", Pattern.CASE_INSENSITIVE);
    private static final Pattern O_SERIES_PREFIX = Pattern.compile("^o\\d+-", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_SUFFIX = Pattern.compile("-\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern SHORT_DATE_SUFFIX = Pattern.compile("-(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])$");
    private static final List<Pattern> EXCLUDED_KEYWORDS = List.of(
            Pattern.compile("embedding", Pattern.CASE_INSENSITIVE),
            Pattern.compile("image", Pattern.CASE_INSENSITIVE),
            Pattern.compile("audio", Pattern.CASE_INSENSITIVE),
            Pattern.compile("realtime", Pattern.CASE_INSENSITIVE),
            Pattern.compile("vision-preview", Pattern.CASE_INSENSITIVE),
            Pattern.compile("-preview", Pattern.CASE_INSENSITIVE),
            Pattern.compile("tts", Pattern.CASE_INSENSITIVE),
            Pattern.compile("whisper", Pattern.CASE_INSENSITIVE));

    private final OpenAiClientFactory clientFactory;

    public OpenAiModelCatalogClient(OpenAiClientFactory clientFactory) {
        this.clientFactory = clientFactory;
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
            logger.info("Fetching OpenAI model catalog via SDK");
            OpenAI client = clientFactory.createClient(apiKey);
            ModelListResponse response = client.models().list();
            if (response == null || response.data() == null) {
                logger.warn("Model catalog response was empty");
                throw new OpenAiIntegrationException("OpenAIモデル一覧を取得できませんでした。");
            }

            List<String> models = filterAllowedModels(response.data().stream()
                    .map(Model::id)
                    .collect(Collectors.toList()));

            logger.info("Retrieved {} models from OpenAI", models.size());
            return models;
        } catch (OpenAIException ex) {
            logger.warn("OpenAI model catalog request failed: status={}, code={}, message={}",
                    safeStatus(ex), safeCode(ex), ex.getMessage());
            throw new OpenAiIntegrationException("OpenAIモデル一覧の取得に失敗しました。", ex);
        } catch (RuntimeException ex) {
            logger.warn("OpenAI model catalog request error: {}", ex.getMessage());
            throw new OpenAiIntegrationException("OpenAIモデル一覧の取得中にエラーが発生しました。", ex);
        }
    }

    List<String> filterAllowedModels(List<String> modelIds) {
        return modelIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(modelId -> !modelId.isEmpty())
                .filter(modelId -> GPT_PREFIX.matcher(modelId).find() || O_SERIES_PREFIX.matcher(modelId).find())
                .filter(modelId -> !DATE_SUFFIX.matcher(modelId).find())
                .filter(modelId -> !SHORT_DATE_SUFFIX.matcher(modelId).find())
                .filter(modelId -> EXCLUDED_KEYWORDS.stream()
                        .noneMatch(pattern -> pattern.matcher(modelId).find()))
                .distinct()
                .sorted(Comparator
                        .comparing((String modelId) -> modelId.toLowerCase(Locale.ROOT))
                        .thenComparing(Comparator.naturalOrder()))
                .collect(Collectors.toList());
    }

    private static String safeStatus(OpenAIException ex) {
        if (ex == null) {
            return "n/a";
        }
        Integer statusCode = ex.getStatusCode();
        return statusCode == null ? "n/a" : statusCode.toString();
    }

    private static String safeCode(OpenAIException ex) {
        if (ex == null || ex.getError() == null) {
            return null;
        }
        String code = ex.getError().getCode();
        return code == null || code.isBlank() ? null : code;
    }
}
