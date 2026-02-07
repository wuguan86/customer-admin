package com.shijie.transit.userapi.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DifyClient {
  private final DifyProperties properties;
  private final ObjectMapper objectMapper;

  public DifyClient(DifyProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public DifyChatResult chatMessages(String requestBodyJson) {
    String responseJson = restClient().post()
        .uri("/v1/chat-messages")
        .contentType(MediaType.APPLICATION_JSON)
        .body(requestBodyJson)
        .retrieve()
        .body(String.class);
    return parseChatResult(responseJson);
  }

  public String getDocument(String datasetId, String documentId) {
    return restClient().get()
        .uri("/v1/datasets/{datasetId}/documents/{documentId}", datasetId, documentId)
        .retrieve()
        .body(String.class);
  }

  public String uploadDocumentByFile(String datasetId, String dataJson, MultipartFile file) throws IOException {
    MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
    multipartBody.add("data", dataJson);
    multipartBody.add("file", new ByteArrayResource(file.getBytes()) {
      @Override
      public String getFilename() {
        return file.getOriginalFilename();
      }
    });

    return restClient().post()
        .uri("/v1/datasets/{datasetId}/document/create-by-file", datasetId)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(multipartBody)
        .retrieve()
        .body(String.class);
  }

  private RestClient restClient() {
    String apiKey = properties.getApiKey();
    return RestClient.builder()
        .baseUrl(properties.getBaseUrl())
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .build();
  }

  private DifyChatResult parseChatResult(String responseJson) {
    if (responseJson == null) {
      return new DifyChatResult(null, null, null);
    }
    try {
      JsonNode node = objectMapper.readTree(responseJson);
      String conversationId = node.hasNonNull("conversation_id") ? node.get("conversation_id").asText() : null;
      String answer = node.hasNonNull("answer") ? node.get("answer").asText() : null;
      return new DifyChatResult(responseJson, conversationId, answer);
    } catch (Exception ex) {
      return new DifyChatResult(responseJson, null, null);
    }
  }

  public record DifyChatResult(String rawJson, String conversationId, String answer) {
  }
}
