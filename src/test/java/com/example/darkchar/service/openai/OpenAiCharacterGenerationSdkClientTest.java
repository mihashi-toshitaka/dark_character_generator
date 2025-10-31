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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.WorldGenre;
import com.openai.client.OpenAI;
import com.openai.exceptions.OpenAIException;
import com.openai.models.Response;
import com.openai.models.ResponseCreateParams;
import com.openai.models.ResponseOutput;
import com.openai.models.ResponseOutputContent;
import com.openai.models.ResponseOutputContentText;
import com.openai.resources.responses.ResponsesClient;

class OpenAiCharacterGenerationSdkClientTest {

    private OpenAiClientFactory clientFactory;
    private OpenAI openAi;
    private ResponsesClient responsesClient;
    private OpenAiCharacterGenerationSdkClient client;

    @BeforeEach
    void setUp() {
        clientFactory = mock(OpenAiClientFactory.class);
        openAi = mock(OpenAI.class);
        responsesClient = mock(ResponsesClient.class);

        when(clientFactory.createClient("test-key")).thenReturn(openAi);
        when(openAi.responses()).thenReturn(responsesClient);

        client = new OpenAiCharacterGenerationSdkClient(clientFactory);
    }

    @Test
    void generateNarrativeCombinesOutputText() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        Response response = mock(Response.class);
        ResponseOutput output = mock(ResponseOutput.class);
        ResponseOutputContent content = mock(ResponseOutputContent.class);
        ResponseOutputContentText text = mock(ResponseOutputContentText.class);

        when(responsesClient.create(any(ResponseCreateParams.class))).thenReturn(response);
        when(response.output()).thenReturn(List.of(output));
        when(output.content()).thenReturn(List.of(content));
        when(content.text()).thenReturn(text);
        when(text.value()).thenReturn("第一段落。\n第二段落。\nそして終わり。\n");

        String actual = client.generateNarrative("test-key", "gpt-test", input, selection);

        assertThat(actual).isEqualTo("第一段落。\n第二段落。\nそして終わり。");

        verify(responsesClient, times(1)).create(any(ResponseCreateParams.class));
        verifyNoMoreInteractions(responsesClient);
    }

    @Test
    void generateNarrativeRetriesWithoutTemperatureWhenUnsupported() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        Response response = mock(Response.class);
        ResponseOutput output = mock(ResponseOutput.class);
        ResponseOutputContent content = mock(ResponseOutputContent.class);
        ResponseOutputContentText text = mock(ResponseOutputContentText.class);

        when(responsesClient.create(any(ResponseCreateParams.class)))
                .thenThrow(mockTemperatureUnsupportedException())
                .thenReturn(response);
        when(response.output()).thenReturn(List.of(output));
        when(output.content()).thenReturn(List.of(content));
        when(content.text()).thenReturn(text);
        when(text.value()).thenReturn("最初の行。\n次の行。\n");

        String actual = client.generateNarrative("test-key", "gpt-test", input, selection);

        assertThat(actual).isEqualTo("最初の行。\n次の行。");

        ArgumentCaptor<ResponseCreateParams> requestCaptor = ArgumentCaptor.forClass(ResponseCreateParams.class);
        verify(responsesClient, times(2)).create(requestCaptor.capture());
        List<ResponseCreateParams> requests = requestCaptor.getAllValues();
        assertThat(requests.get(0).temperature()).isEqualTo(0.8d);
        assertThat(requests.get(1).temperature()).isNull();
    }

    @Test
    void generateNarrativeThrowsWhenErrorDoesNotIndicateTemperatureIssue() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        OpenAIException exception = mock(OpenAIException.class);
        when(exception.getMessage()).thenReturn("The provided prompt is invalid.");

        when(responsesClient.create(any(ResponseCreateParams.class))).thenThrow(exception);

        assertThatThrownBy(() -> client.generateNarrative("test-key", "gpt-test", input, selection))
                .isInstanceOf(OpenAiIntegrationException.class)
                .hasMessageContaining("OpenAI API呼び出しに失敗しました。");
    }

    private CharacterInput createCharacterInput() {
        return new CharacterInput(
                InputMode.SEMI_AUTO,
                new WorldGenre(1L, "ダークファンタジー"),
                List.of(new AttributeOption(1L, AttributeCategory.CHARACTER_TRAIT, "堕ちた騎士", "名誉を失った騎士")),
                "秘密の弱み",
                4,
                "影に魅入られた");
    }

    private DarknessSelection createDarknessSelection() {
        return new DarknessSelection(
                Map.of(AttributeCategory.MINDSET,
                        List.of(new AttributeOption(2L, AttributeCategory.MINDSET, "復讐心", "復讐に燃える"))),
                3);
    }

    private OpenAIException mockTemperatureUnsupportedException() {
        OpenAIException exception = mock(OpenAIException.class);
        when(exception.getMessage()).thenReturn("'temperature' is not supported for this model.");
        return exception;
    }
}
