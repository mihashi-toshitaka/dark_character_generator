package com.example.darkchar.service.openai;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OpenAI が提供するテキスト生成モデルの列挙。
 */
public enum OpenAiGenerationModel {

    GPT_4O("gpt-4o"),
    GPT_4O_MINI("gpt-4o-mini"),
    GPT_4_1("gpt-4.1"),
    GPT_4_1_MINI("gpt-4.1-mini"),
    GPT_4O_2024_05_13("gpt-4o-2024-05-13"),
    GPT_4O_MINI_2024_05_13("gpt-4o-mini-2024-05-13");

    private final String id;

    OpenAiGenerationModel(String id) {
        this.id = id;
    }

    /**
     * モデル ID を返します。
     *
     * @return OpenAI モデル ID
     */
    public String getId() {
        return id;
    }

    /**
     * モデル ID のストリームを返します。
     *
     * @return モデル ID のストリーム
     */
    public static Stream<String> stream() {
        return Arrays.stream(values()).map(OpenAiGenerationModel::getId);
    }

    /**
     * モデル ID の一覧を返します。
     *
     * @return モデル ID の一覧
     */
    public static List<String> ids() {
        return stream().collect(Collectors.toUnmodifiableList());
    }
}
