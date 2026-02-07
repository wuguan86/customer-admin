package com.shijie.transit.userapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shijie.transit.common.security.TransitPrincipal;
import com.shijie.transit.userapi.dify.DifyClient;
import com.shijie.transit.userapi.service.DifyMappingService;

import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/dify")
public class UserDifyController {
  private final DifyClient difyClient;
  private final DifyMappingService mappingService;
  private final ObjectMapper objectMapper;

  public UserDifyController(DifyClient difyClient, DifyMappingService mappingService, ObjectMapper objectMapper) {
    this.difyClient = difyClient;
    this.mappingService = mappingService;
    this.objectMapper = objectMapper;
  }

  @PostMapping(value = "/chat-messages", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public String chatMessages(@RequestBody String requestJson) throws IOException {
    TransitPrincipal principal = currentPrincipal();
    String adjusted = adjustChatRequest(principal, requestJson);
    DifyClient.DifyChatResult result = difyClient.chatMessages(adjusted);
    if (result.conversationId() != null && !result.conversationId().isBlank()) {
      mappingService.recordConversation(principal.subjectId(), result.conversationId());
    }
    return result.rawJson();
  }

  @GetMapping("/kb")
  public Map<String, String> getKnowledgeBase() {
    TransitPrincipal principal = currentPrincipal();
    String kbId = mappingService.getBoundKnowledgeBase(principal.subjectId());
    return Map.of("knowledgeBaseId", kbId == null ? "" : kbId);
  }

  @PutMapping("/kb")
  public void bindKnowledgeBase(@RequestBody BindKnowledgeBaseRequest request) {
    TransitPrincipal principal = currentPrincipal();
    mappingService.bindKnowledgeBase(principal.subjectId(), request.knowledgeBaseId());
  }

  @GetMapping(value = "/datasets/{datasetId}/documents/{documentId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getDocument(@PathVariable("datasetId") String datasetId, @PathVariable("documentId") String documentId) {
    return difyClient.getDocument(datasetId, documentId);
  }

  @PostMapping(
      value = "/datasets/{datasetId}/document/create-by-file",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public String uploadDocument(
      @PathVariable("datasetId") String datasetId,
      @RequestPart("data") String data,
      @RequestPart("file") MultipartFile file) throws IOException {
    return difyClient.uploadDocumentByFile(datasetId, data, file);
  }

  private String adjustChatRequest(TransitPrincipal principal, String requestJson) throws IOException {
    JsonNode node = objectMapper.readTree(requestJson);
    if (!(node instanceof ObjectNode obj)) {
      return requestJson;
    }

    if (!obj.hasNonNull("user")) {
      obj.put("user", "user-" + principal.subjectId());
    }

    if (!obj.hasNonNull("conversation_id")) {
      String latest = mappingService.getLatestConversationId(principal.subjectId());
      if (latest != null && !latest.isBlank()) {
        obj.put("conversation_id", latest);
      }
    }
    return objectMapper.writeValueAsString(obj);
  }

  private TransitPrincipal currentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return (TransitPrincipal) authentication.getPrincipal();
  }

  public record BindKnowledgeBaseRequest(String knowledgeBaseId) {
  }
}
