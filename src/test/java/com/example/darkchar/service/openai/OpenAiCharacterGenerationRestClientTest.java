package com.example.darkchar.service.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.darkchar.domain.AttributeCategory;
import com.example.darkchar.domain.AttributeOption;
import com.example.darkchar.domain.CharacterInput;
import com.example.darkchar.domain.DarknessSelection;
import com.example.darkchar.domain.InputMode;
import com.example.darkchar.domain.WorldGenre;
import com.fasterxml.jackson.databind.ObjectMapper;

class OpenAiCharacterGenerationRestClientTest {

    private static final String BASE_URL = "https://api.openai.com/v1";

    private MockRestServiceServer mockServer;
    private OpenAiCharacterGenerationRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        client = new OpenAiCharacterGenerationRestClient(builder, new ObjectMapper());
    }

    @Test
    void generateNarrativeCombinesMultilineOutput() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        String responseJson = """
                {
                  \"id\": \"resp-123\",
                  \"model\": \"gpt-test\",
                  \"created\": 1710000000,
                  \"usage\": {
                    \"input_tokens\": 100,
                    \"output_tokens\": 200,
                    \"total_tokens\": 300
                  },
                  \"output\": [
                    {
                      \"content\": [
                        {\"type\": \"output_text\", \"text\": \"第一段落。\\n\"},
                        {\"type\": \"output_text\", \"text\": \"第二段落。\\nそして終わり。\"}
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(ExpectedCount.once(), requestTo(BASE_URL + "/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("gpt-test"))
                .andExpect(jsonPath("$.temperature").value(0.8))
                .andExpect(jsonPath("$.max_output_tokens").value(600))
                .andExpect(jsonPath("$.input[0].role").value("user"))
                .andExpect(jsonPath("$.input[0].content[0].type").value("input_text"))
                .andExpect(jsonPath("$.input[0].content[0].text").value(Matchers.containsString("あなたは闇堕ちキャラクター")))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String actual = client.generateNarrative("test-key", "gpt-test", input, selection);

        assertThat(actual).isEqualTo("第一段落。\n第二段落。\nそして終わり。");
        mockServer.verify();
    }

    @Test
    void generateNarrativeFlattensNestedMessageContent() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        String responseJson = """
                {
                  \"id\": \"resp-789\",
                  \"model\": \"gpt-5o\",
                  \"created\": 1710000002,
                  \"output\": [
                    {
                      \"content\": [
                        {
                          \"type\": \"message\",
                          \"role\": \"assistant\",
                          \"content\": [
                            {\"type\": \"text\", \"text\": {\"value\": \"第一章。\\n\"}},
                            {\"type\": \"text\", \"text\": {\"value\": \"第二章。\"}}
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(ExpectedCount.once(), requestTo(BASE_URL + "/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String actual = client.generateNarrative("test-key", "gpt-5o", input, selection);

        assertThat(actual).isEqualTo("第一章。\n第二章。");
        mockServer.verify();
    }

    @Test
    void generateNarrativeRetriesWithoutTemperatureWhenUnsupported() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        String errorBody = """
                {
                  \"error\": {
                    \"type\": \"invalid_request_error\",
                    \"code\": null,
                    \"message\": \"Unsupported parameter: 'temperature' is not supported with this model.\",
                    \"param\": \"temperature\"
                  }
                }
                """;

        String successBody = """
                {
                  \"id\": \"resp-456\",
                  \"model\": \"gpt-test\",
                  \"created\": 1710000001,
                  \"output\": [
                    {
                      \"content\": [
                        {\"type\": \"output_text\", \"text\": \"最初の行。\\n\"},
                        {\"type\": \"output_text\", \"text\": \"次の行。\"}
                      ]
                    }
                  ]
                }
                """;

        mockServer.expect(ExpectedCount.once(), requestTo(BASE_URL + "/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.temperature").value(0.8))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        mockServer.expect(ExpectedCount.once(), requestTo(BASE_URL + "/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.temperature").doesNotExist())
                .andRespond(withSuccess(successBody, MediaType.APPLICATION_JSON));

        String actual = client.generateNarrative("test-key", "gpt-test", input, selection);

        assertThat(actual).isEqualTo("最初の行。\n次の行。");
        mockServer.verify();
    }

    @Test
    void generateNarrativeDoesNotRetryForUnrelatedError() {
        CharacterInput input = createCharacterInput();
        DarknessSelection selection = createDarknessSelection();

        String errorBody = """
                {
                  \"error\": {
                    \"type\": \"invalid_request_error\",
                    \"code\": \"bad_request\",
                    \"message\": \"The provided prompt is invalid.\"
                  }
                }
                """;

        mockServer.expect(ExpectedCount.once(), requestTo(BASE_URL + "/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andExpect(jsonPath("$.temperature").value(0.8))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        assertThatThrownBy(() -> client.generateNarrative("test-key", "gpt-test", input, selection))
                .isInstanceOf(OpenAiIntegrationException.class)
                .hasMessageContaining("HTTP 400")
                .hasMessageContaining("bad_request");

        mockServer.verify();
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
}
