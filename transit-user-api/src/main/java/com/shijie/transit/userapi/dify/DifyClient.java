package com.shijie.transit.userapi.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shijie.transit.common.web.ErrorCode;
import com.shijie.transit.common.web.TransitException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DifyClient {
  private static final Logger log = LoggerFactory.getLogger(DifyClient.class);
  private final DifyProperties properties;
  private final ObjectMapper objectMapper;

  public DifyClient(DifyProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public DifyChatResult chatMessages(String requestBodyJson) {
    try {
      log.info("Dify chatMessages requestSize={}", requestBodyJson == null ? 0 : requestBodyJson.length());
      String responseJson = restClientForChat().post()
          .uri("/v1/chat-messages")
          .contentType(MediaType.APPLICATION_JSON)
          .body(requestBodyJson)
          .retrieve()
          .body(String.class);
      log.info("Dify chatMessages responseSize={}", responseJson == null ? 0 : responseJson.length());
      return parseChatResult(responseJson);
    } catch (RestClientResponseException ex) {
      throw toTransitException(ex);
    }
  }

  public DifyDatasetResult createDataset(String name) {
    try {
      log.info("Dify createDataset name={}", name);
      ObjectNode request = objectMapper.createObjectNode();
      request.put("name", name);
      request.put("permission", "only_me");

      String responseJson = restClientForDataset().post()
          .uri("/v1/datasets")
          .contentType(MediaType.APPLICATION_JSON)
          .body(request.toString())
          .retrieve()
          .body(String.class);
      log.info("Dify createDataset responseSize={}", responseJson == null ? 0 : responseJson.length());
      return parseDatasetResult(responseJson);
    } catch (RestClientResponseException ex) {
      throw toTransitException(ex);
    }
  }

  public String getDocument(String datasetId, String documentId) {
    try {
      log.info("Dify getDocument datasetId={} documentId={}", datasetId, documentId);
      return restClientForDataset().get()
          .uri("/v1/datasets/{datasetId}/documents/{documentId}", datasetId, documentId)
          .retrieve()
          .body(String.class);
    } catch (RestClientResponseException ex) {
      throw toTransitException(ex);
    }
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

    try {
      log.info("Dify uploadDocument datasetId={} fileName={}", datasetId, file.getOriginalFilename());
      return restClientForDataset().post()
          .uri("/v1/datasets/{datasetId}/document/create-by-file", datasetId)
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(multipartBody)
          .retrieve()
          .body(String.class);
    } catch (RestClientResponseException ex) {
      throw toTransitException(ex);
    }
  }

  public String retrieveDataset(String datasetId, String query) {
    try {
      log.info("Dify retrieve datasetId={} querySize={}", datasetId, query == null ? 0 : query.length());
      ObjectNode request = objectMapper.createObjectNode();
      request.put("query", query);

//      ObjectNode retrievalModel = objectMapper.createObjectNode();
//      retrievalModel.put("search_method", "keyword_search");
//      retrievalModel.put("reranking_enable", false);
//      retrievalModel.putNull("reranking_mode");
//      ObjectNode rerankingModel = objectMapper.createObjectNode();
//      rerankingModel.put("reranking_provider_name", "");
//      rerankingModel.put("reranking_model_name", "");
//      retrievalModel.set("reranking_model", rerankingModel);
//      retrievalModel.putNull("weights");
//      retrievalModel.put("top_k", 3);
//      retrievalModel.put("score_threshold_enabled", false);
//      retrievalModel.putNull("score_threshold");
//      request.set("retrieval_model", retrievalModel);

      return restClientForDataset().post()
          .uri("/v1/datasets/{datasetId}/retrieve", datasetId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(request.toString())
          .retrieve()
          .body(String.class);
    } catch (RestClientResponseException ex) {
      throw toTransitException(ex);
    }
  }

  private RestClient restClientForChat() {
    String baseUrl = properties.getBaseUrl();
    String apiKey = properties.getChatApiKey();
    if (!StringUtils.hasText(baseUrl)) {
      throw new TransitException(ErrorCode.INTERNAL_ERROR, "DIFY_BASE_URL 未配置");
    }
    if (!StringUtils.hasText(apiKey)) {
      throw new TransitException(ErrorCode.UNAUTHORIZED, "DIFY_CHAT_API_KEY 未配置");
    }
    return RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .build();
  }

  private RestClient restClientForDataset() {
    String baseUrl = properties.getBaseUrl();
    String apiKey = properties.getDatasetApiKey();
    if (!StringUtils.hasText(baseUrl)) {
      throw new TransitException(ErrorCode.INTERNAL_ERROR, "DIFY_BASE_URL 未配置");
    }
    if (!StringUtils.hasText(apiKey)) {
      throw new TransitException(ErrorCode.UNAUTHORIZED, "DIFY_DATASET_API_KEY 未配置");
    }
    return RestClient.builder()
        .baseUrl(baseUrl)
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

  private DifyDatasetResult parseDatasetResult(String responseJson) {
    if (responseJson == null) {
      return new DifyDatasetResult(null, null);
    }
    try {
      JsonNode node = objectMapper.readTree(responseJson);
      String datasetId = node.hasNonNull("id") ? node.get("id").asText() : null;
      return new DifyDatasetResult(responseJson, datasetId);
    } catch (Exception ex) {
      return new DifyDatasetResult(responseJson, null);
    }
  }

  public record DifyChatResult(String rawJson, String conversationId, String answer) {
  }

  public record DifyDatasetResult(String rawJson, String datasetId) {
  }

  private TransitException toTransitException(RestClientResponseException ex) {
    ErrorCode errorCode = switch (ex.getRawStatusCode()) {
      case 401 -> ErrorCode.UNAUTHORIZED;
      case 403 -> ErrorCode.FORBIDDEN;
      default -> ex.getRawStatusCode() >= 500 ? ErrorCode.INTERNAL_ERROR : ErrorCode.BAD_REQUEST;
    };

    String message = null;
    try {
      String body = ex.getResponseBodyAsString();
      if (body != null && !body.isBlank()) {
        JsonNode node = objectMapper.readTree(body);
        if (node.hasNonNull("message")) {
          message = node.get("message").asText();
        } else if (node.hasNonNull("error")) {
          message = node.get("error").asText();
        }
      }
    } catch (Exception ignored) {
      message = null;
    }

    String finalMessage = (message == null || message.isBlank())
        ? "Dify API error (" + ex.getRawStatusCode() + ")"
        : "Dify API error (" + ex.getRawStatusCode() + "): " + message;
    return new TransitException(errorCode, finalMessage, ex);
  }
}
