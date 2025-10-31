package com.example.darkchar.service.openai;

import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.service.ai.ProviderGenerationResult;

/**
 * OpenAI APIを用いてキャラクター生成テキストを取得するクライアントの抽象化。
 */
public interface OpenAiCharacterGenerationClient {

    /**
     * 入力情報を元にOpenAIへ問い合わせ、生成されたテキストを返します。
     *
     * @param apiKey    使用するAPIキー
     * @param input     キャラクター入力
     * @param selection 闇堕ち選択情報
     * @return 生成されたテキスト
     * @throws OpenAiIntegrationException OpenAI連携に失敗した場合
     */
    ProviderGenerationResult generate(String apiKey, String modelId, CharacterInput input, DarknessSelection selection);
}
