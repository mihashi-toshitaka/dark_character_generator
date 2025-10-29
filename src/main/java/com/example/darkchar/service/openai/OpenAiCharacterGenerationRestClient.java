package com.example.darkchar.service.openai;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link RestClient} を利用した OpenAI 連携クライアント実装。
 */
@Component
public class OpenAiCharacterGenerationRestClient implements OpenAiCharacterGenerationClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCharacterGenerationRestClient.class);
    private final RestClient restClient;

    public OpenAiCharacterGenerationRestClient(@Qualifier("openAiRestClientBuilder") RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    public String generateNarrative(String apiKey, String modelId, CharacterInput input, DarknessSelection selection) {
        String normalizedModel = modelId == null ? null : modelId.trim();
        if (normalizedModel == null || normalizedModel.isEmpty()) {
            throw new OpenAiIntegrationException("OpenAIリクエストに使用するモデルが選択されていません。");
        }
        // 最初は temperature を付けて試行し、APIが未対応の場合は temperature を外して1回だけ再試行する。
    String responseText = null;
        boolean triedWithoutTemperature = false;
        for (int attempt = 0; attempt < 2; attempt++) {
            boolean includeTemperature = (attempt == 0);
            if (!includeTemperature) {
                triedWithoutTemperature = true;
            }
            logger.info("Calling OpenAI responses API: model={}, temperature={}, maxOutputTokens={}",
                    normalizedModel, includeTemperature ? 0.8 : "(omitted)", 600);
            try {
                var requestBody = buildRequestMap(normalizedModel, input, selection, includeTemperature);
                // まず生のJSON文字列を取得しておく（APIのレスポンス構造が変わることがあるため柔軟に対応する）
                String raw = restClient.post()
                        .uri("/responses")
                        .headers(headers -> {
                            headers.setBearerAuth(apiKey);
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                // ログ（必要なら管理者が確認できるように）
                logger.debug("OpenAI raw response: {}", raw);

                // JSON をパースしてテキストを抽出する（柔軟に探索）
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(raw);

                    // 可能なら既存の型にマッピングして usage 等をログ出力
                    try {
                        OpenAiResponse mapped = mapper.treeToValue(root, OpenAiResponse.class);
                        if (mapped != null) {
                            logger.info("Received OpenAI response: id={}, created={}, model={}, usage={}",
                                    mapped.id(), mapped.created(), mapped.model(), formatUsage(mapped.usage()));
                        }
                    } catch (Exception e) {
                        // 型マッピングに失敗しても問題ない
                    }

                    String extracted = extractTextFromJson(root);
                    if (extracted != null && !extracted.isBlank()) {
                        responseText = extracted;
                        // 正常終了すればループを抜ける
                        break;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse OpenAI response JSON: {}", e.getMessage());
                    throw new OpenAiIntegrationException("OpenAIレスポンスの解析に失敗しました。", e);
                }
            } catch (RestClientResponseException ex) {
        logger.warn("OpenAI responses API call failed: status={} {}, model={}", ex.getStatusCode().value(),
            ex.getStatusText(), normalizedModel);
                String respBody = null;
                try {
                    respBody = ex.getResponseBodyAsString();
                    if (respBody != null && !respBody.isBlank()) {
                        logger.warn("OpenAI responses API error body: {}", respBody);
                    }
                } catch (Exception e) {
                    // 無理に取得できなくても続行
                }

                // temperature が未サポートという明示的なエラーなら、次のループで temperature を外して再試行する
                if (!triedWithoutTemperature && respBody != null
                        && respBody.contains("'temperature' is not supported")) {
                    logger.info("Model {} does not support temperature; retrying without temperature.", normalizedModel);
                    continue; // 次のattemptで includeTemperature=false にする
                }

                String message = "OpenAI API呼び出しに失敗しました: HTTP %d %s".formatted(ex.getStatusCode().value(), ex.getStatusText());
                throw new OpenAiIntegrationException(message, ex);
            } catch (RestClientException ex) {
                logger.warn("OpenAI responses API request encountered an error for model {}: {}", normalizedModel,
                        ex.getMessage());
                throw new OpenAiIntegrationException("OpenAI APIへのリクエスト中にエラーが発生しました。", ex);
            }
        }

        if (responseText != null && !responseText.isBlank()) {
            return responseText.trim();
        }

        throw new OpenAiIntegrationException("OpenAIレスポンスからテキストを取得できませんでした。");
    }

    /**
     * リクエストボディを Map として組み立てる。temperature を省略可能にするため Map を返す。
     */
    private Map<String, Object> buildRequestMap(String modelId, CharacterInput input, DarknessSelection selection,
            boolean includeTemperature) {
        String prompt = buildPrompt(input, selection);
        Map<String, String> content = Map.of("type", "input_text", "text", prompt);
        Map<String, Object> message = Map.of("role", "user", "content", List.of(content));
        Map<String, Object> req = new java.util.LinkedHashMap<>();
        req.put("model", modelId);
        req.put("input", List.of(message));
        if (includeTemperature) {
            req.put("temperature", 0.8);
        }
        req.put("max_output_tokens", 600);
        return req;
    }

    private String buildPrompt(CharacterInput input, DarknessSelection selection) {
        StringBuilder builder = new StringBuilder();
        builder.append("あなたは闇堕ちキャラクターの設定と短いストーリーを作成する日本語のライターです。\n");
        builder.append("以下の入力情報に基づき、世界観の説明、キャラクターの転落のきっかけ、闇堕ち後の姿を含む物語を400〜600文字程度で作成してください。\n");
        builder.append("最後に箇条書きは使わず、段落構成でまとめてください。\n\n");

        builder.append("[世界観ジャンル]\n");
        builder.append(input.worldGenre().name()).append("\n\n");

        builder.append("[モード]\n");
        builder.append(input.mode() == InputMode.AUTO ? "オート" : "セミオート").append("\n\n");

        if (input.mode() == InputMode.SEMI_AUTO && input.characterTraits() != null && !input.characterTraits().isEmpty()) {
            builder.append("[キャラクター属性]\n");
            for (AttributeOption option : input.characterTraits()) {
                builder.append("・").append(option.name()).append(": ").append(option.description()).append("\n");
            }
            builder.append("\n");
        }

        if (input.traitFreeText() != null && !input.traitFreeText().isBlank()) {
            builder.append("[キャラクター属性メモ]\n");
            builder.append(input.traitFreeText().trim()).append("\n\n");
        }

        builder.append("[主人公度]\n");
        builder.append(input.protagonistScore()).append("/5\n\n");

        builder.append("[闇堕ちカテゴリと選択肢]\n");
        Map<AttributeCategory, List<AttributeOption>> darknessSelections = selection.selections();
        for (Map.Entry<AttributeCategory, List<AttributeOption>> entry : darknessSelections.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            builder.append(entry.getKey().getDisplayName()).append(": ");
            StringJoiner joiner = new StringJoiner("、");
            for (AttributeOption option : entry.getValue()) {
                joiner.add(option.name());
            }
            builder.append(joiner).append("\n");
        }
        builder.append("\n");

        builder.append("[闇堕ち度]\n");
        builder.append(selection.darknessLevel()).append("/5\n\n");

        if (input.darknessFreeText() != null && !input.darknessFreeText().isBlank()) {
            builder.append("[闇堕ちメモ]\n");
            builder.append(input.darknessFreeText().trim()).append("\n\n");
        }

        builder.append("条件:\n");
        builder.append("1. キャラクターが闇堕ちに至った心理的な揺らぎや事件を描写する。\n");
        builder.append("2. 闇堕ち後のビジュアルや能力、価値観の変化を盛り込む。\n");
        builder.append("3. 最後は読者が次の展開を想像できる余韻を残す。\n");
        builder.append("4. 出力は日本語で行う。\n");

        return builder.toString();
    }

    private String extractText(OpenAiResponse response) {
        if (response == null || response.output() == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (OpenAiOutput output : response.output()) {
            if (output == null || output.content() == null) {
                continue;
            }
            for (OpenAiResponseContent content : output.content()) {
                if (content == null) {
                    continue;
                }
                if (Objects.equals(content.type(), "output_text") || Objects.equals(content.type(), "text")) {
                    if (content.text() != null) {
                        builder.append(content.text());
                    }
                }
            }
        }
        return builder.toString();
    }

    /**
     * JsonNode から柔軟にテキストを抽出する。Responses API の出力構造は変わることがあるため、
     * output -> content の各ノードを再帰的に探索して text フィールドを収集する。
     */
    private String extractTextFromJson(com.fasterxml.jackson.databind.JsonNode root) {
        if (root == null) {
            return null;
        }
        var sb = new StringBuilder();
        var outputs = root.path("output");
        if (outputs.isMissingNode() || !outputs.isArray()) {
            // まれに top-level に直接 text がある場合も探す
            collectTextNodes(root, sb);
            return sb.length() == 0 ? null : sb.toString();
        }
        for (var out : outputs) {
            if (out == null || out.isMissingNode())
                continue;
            var contents = out.path("content");
            if (contents != null && contents.isArray()) {
                for (var c : contents) {
                    collectTextNodes(c, sb);
                }
            } else {
                collectTextNodes(out, sb);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private void collectTextNodes(com.fasterxml.jackson.databind.JsonNode node, StringBuilder sb) {
        if (node == null || node.isMissingNode())
            return;
        if (node.has("text") && node.get("text").isTextual()) {
            sb.append(node.get("text").asText());
            return;
        }
        if (node.isTextual()) {
            sb.append(node.asText());
            return;
        }
        if (node.isObject()) {
            var it = node.fields();
            while (it.hasNext()) {
                var e = it.next();
                collectTextNodes(e.getValue(), sb);
            }
            return;
        }
        if (node.isArray()) {
            for (var el : node) {
                collectTextNodes(el, sb);
            }
        }
    }

    private String formatUsage(OpenAiUsage usage) {
        if (usage == null) {
            return "usage=unknown";
        }
        return "prompt=%s, completion=%s, total=%s".formatted(
                usage.promptTokens() == null ? "?" : usage.promptTokens(),
                usage.completionTokens() == null ? "?" : usage.completionTokens(),
                usage.totalTokens() == null ? "?" : usage.totalTokens());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiRequest(
            String model,
            List<OpenAiMessage> input,
            double temperature,
            @JsonProperty("max_output_tokens") int maxOutputTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiMessage(String role, List<OpenAiContent> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiContent(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiResponse(
            String id,
            String model,
            Long created,
            OpenAiUsage usage,
            List<OpenAiOutput> output) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiOutput(List<OpenAiResponseContent> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiResponseContent(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiUsage(
            @JsonProperty("input_tokens") Integer promptTokens,
            @JsonProperty("output_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {
    }
}
