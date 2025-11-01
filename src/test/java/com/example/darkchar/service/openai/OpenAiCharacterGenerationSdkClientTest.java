package com.example.darkchar.service.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.WorldGenre;
import com.example.darkchar.service.ai.ProviderGenerationResult;
import com.openai.client.OpenAIClient;
import com.openai.errors.BadRequestException;
import com.openai.errors.OpenAIException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.services.blocking.ChatService;
import com.openai.services.blocking.chat.ChatCompletionService;

/**
 * {@link OpenAiCharacterGenerationSdkClient} の振る舞いを検証します。
 */
class OpenAiCharacterGenerationSdkClientTest {

    private OpenAiClientFactory clientFactory;
    private OpenAIClient openAiClient;
    private ChatService chatService;
    private ChatCompletionService chatCompletionService;
    private PromptTemplateRenderer promptTemplateRenderer;
    private OpenAiCharacterGenerationSdkClient client;

    /**
     * 各テストで必要なスタブを初期化します。
     */
    @BeforeEach
    void setUp() {
        clientFactory = mock(OpenAiClientFactory.class);
        openAiClient = mock(OpenAIClient.class);
        chatService = mock(ChatService.class);
        chatCompletionService = mock(ChatCompletionService.class);
        promptTemplateRenderer = mock(PromptTemplateRenderer.class);

        when(clientFactory.createClient("test-key")).thenReturn(openAiClient);
        when(openAiClient.chat()).thenReturn(chatService);
        when(chatService.completions()).thenReturn(chatCompletionService);
        when(promptTemplateRenderer.render(any(CharacterInput.class), any(DarknessSelection.class)))
                .thenReturn("PROMPT");

        client = new OpenAiCharacterGenerationSdkClient(clientFactory, promptTemplateRenderer);
    }

    /**
     * 正常系でレスポンスの文章を整形して返すことを確認します。
     */
    @Test
    void generateNarrativeCombinesOutputText() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        ChatCompletion chatCompletion = mock(ChatCompletion.class);
        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class);
        ChatCompletionMessage message = mock(ChatCompletionMessage.class);

        when(chatCompletionService.create(any(ChatCompletionCreateParams.class))).thenReturn(chatCompletion);
        when(chatCompletion.choices()).thenReturn(List.of(choice));
        when(choice.message()).thenReturn(message);
        when(message.content()).thenReturn(Optional.of("第一段落。\n第二段落。\nそして終わり。\n"));

        ProviderGenerationResult actual = client.generate("test-key", "gpt-test", input, selection);

        assertThat(actual.narrative()).isEqualTo("第一段落。\n第二段落。\nそして終わり。");
        assertThat(actual.prompt()).hasValue("PROMPT");

        verify(chatCompletionService, times(1)).create(any(ChatCompletionCreateParams.class));
        verifyNoMoreInteractions(chatCompletionService);
        verify(promptTemplateRenderer, times(1)).render(input, selection);
    }

    /**
     * 温度未対応のモデルでリトライすることを確認します。
     */
    @Test
    void generateNarrativeRetriesWithoutTemperatureWhenUnsupported() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        ChatCompletion chatCompletion = mock(ChatCompletion.class);
        ChatCompletion.Choice choice = mock(ChatCompletion.Choice.class);
        ChatCompletionMessage message = mock(ChatCompletionMessage.class);

        when(chatCompletionService.create(any(ChatCompletionCreateParams.class)))
                .thenThrow(mockTemperatureUnsupportedException())
                .thenReturn(chatCompletion);
        when(chatCompletion.choices()).thenReturn(List.of(choice));
        when(choice.message()).thenReturn(message);
        when(message.content()).thenReturn(Optional.of("最初の行。\n次の行。\n"));

        ProviderGenerationResult actual = client.generate("test-key", "gpt-test", input, selection);

        assertThat(actual.narrative()).isEqualTo("最初の行。\n次の行。");
        assertThat(actual.prompt()).hasValue("PROMPT");

        ArgumentCaptor<ChatCompletionCreateParams> requestCaptor = ArgumentCaptor
                .forClass(ChatCompletionCreateParams.class);
        verify(chatCompletionService, times(2)).create(requestCaptor.capture());
        List<ChatCompletionCreateParams> requests = requestCaptor.getAllValues();
        assertThat(requests.get(0).temperature()).isEqualTo(0.8d);
        assertThat(requests.get(1).temperature()).isNull();
        verify(promptTemplateRenderer, times(1)).render(input, selection);
    }

    /**
     * その他のエラーでは例外を伝播することを確認します。
     */
    @Test
    void generateNarrativeThrowsWhenErrorDoesNotIndicateTemperatureIssue() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        OpenAIException exception = mock(OpenAIException.class);
        when(exception.getMessage()).thenReturn("The provided prompt is invalid.");

        when(chatCompletionService.create(any(ChatCompletionCreateParams.class))).thenThrow(exception);

        assertThatThrownBy(() -> client.generate("test-key", "gpt-test", input, selection))
                .isInstanceOf(OpenAiIntegrationException.class)
                .hasMessageContaining("OpenAI API呼び出しに失敗しました。");
    }

    /**
     * テスト用の入力データを作成します。
     *
     * @return キャラクター入力
     */
    private CharacterInput createCharacterInput() {
        return new CharacterInput(
                InputMode.SEMI_AUTO,
                new WorldGenre(1L, "ダークファンタジー"),
                List.of(new AttributeOption(1L, AttributeCategory.CHARACTER_TRAIT, "堕ちた騎士", "名誉を失った騎士")),
                "秘密の弱み",
                4,
                "影に魅入られた");
    }

    /**
     * テスト用の闇堕ち選択肢を作成します。
     *
     * @return 闇堕ち選択
     */
    private DarknessSelection createDarknessSelection() {
        return new DarknessSelection(
                Map.of(AttributeCategory.MINDSET,
                        List.of(new AttributeOption(2L, AttributeCategory.MINDSET, "復讐心", "復讐に燃える"))),
                3);
    }

    /**
     * 温度非対応エラーを模した例外を生成します。
     *
     * @return 温度未対応例外
     */
    private BadRequestException mockTemperatureUnsupportedException() {
        BadRequestException exception = mock(BadRequestException.class);
        when(exception.getMessage()).thenReturn("'temperature' is not supported for this model.");
        return exception;
    }
}
