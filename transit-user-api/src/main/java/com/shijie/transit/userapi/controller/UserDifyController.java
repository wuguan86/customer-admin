package com.shijie.transit.userapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shijie.transit.common.security.TransitPrincipal;
import com.shijie.transit.common.db.entity.TaskEntity;
import com.shijie.transit.common.web.TransitException;
import com.shijie.transit.userapi.dify.DifyClient;
import com.shijie.transit.userapi.service.DifyContactConversationMappingService;
import com.shijie.transit.userapi.service.DifyMappingService;
import com.shijie.transit.userapi.service.TaskService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
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
  private static final Logger log = LoggerFactory.getLogger(UserDifyController.class);
  private final DifyClient difyClient;
  private final DifyMappingService mappingService;
  private final DifyContactConversationMappingService contactConversationMappingService;
  private final TaskService taskService;
  private final ObjectMapper objectMapper;

  public UserDifyController(
      DifyClient difyClient,
      DifyMappingService mappingService,
      DifyContactConversationMappingService contactConversationMappingService,
      TaskService taskService,
      ObjectMapper objectMapper) {
    this.difyClient = difyClient;
    this.mappingService = mappingService;
    this.contactConversationMappingService = contactConversationMappingService;
    this.taskService = taskService;
    this.objectMapper = objectMapper;
  }

  @PostMapping(value = "/chat-messages", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public String chatMessages(@RequestBody String requestJson) throws IOException {
    TransitPrincipal principal = currentPrincipal();
    log.info("Dify chatMessages start userId={} payloadSize={}", principal.subjectId(),
        requestJson == null ? 0 : requestJson.length());
    String adjusted = adjustChatRequest(principal, requestJson);
    DifyClient.DifyChatResult result;
    try {
      result = difyClient.chatMessages(adjusted);
    } catch (TransitException ex) {
      if (isInvalidConversation(ex) && hasConversationId(adjusted)) {
        String stripped = stripConversationId(adjusted);
        log.info("Dify chatMessages retry without conversation userId={}", principal.subjectId());
        result = difyClient.chatMessages(stripped);
      } else {
        throw ex;
      }
    }
    if (result.conversationId() != null && !result.conversationId().isBlank()) {
      mappingService.recordConversation(principal.subjectId(), result.conversationId());
    }
    log.info("Dify chatMessages done userId={} conversationId={} answerSize={}", principal.subjectId(),
        result.conversationId(), result.answer() == null ? 0 : result.answer().length());
    return result.rawJson();
  }

  @PostMapping(value = "/monitor-chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public String monitorChat(@RequestBody MonitorChatRequest request) throws IOException {
    TransitPrincipal principal = currentPrincipal();
    if (request == null || !StringUtils.hasText(request.message()) || request.taskId() == null) {
      log.info("MonitorChat invalid request userId={}", principal.subjectId());
      return "{}";
    }

    boolean hasContact = StringUtils.hasText(request.wechatContact());
    log.info("MonitorChat start userId={} taskId={} messageSize={} hasContact={} conversationIdPresent={}",
        principal.subjectId(), request.taskId(), request.message().length(),
        hasContact,
        StringUtils.hasText(request.conversationId()));
    TaskEntity task = taskService.getById(principal.subjectId(), request.taskId());
    String datasetId = ensureTaskKnowledgeBase(task);
    log.info("MonitorChat dataset resolved userId={} taskId={} datasetId={}", principal.subjectId(),
        request.taskId(), datasetId);
    String retrieveJson = difyClient.retrieveDataset(datasetId, request.message());
    log.info("MonitorChat retrieve done userId={} taskId={} datasetId={} retrieveSize={}",
        principal.subjectId(), request.taskId(), datasetId, retrieveJson == null ? 0 : retrieveJson.length());
    String context = buildContextFromRetrieve(retrieveJson);
    boolean hasTaskRole = StringUtils.hasText(task.getContent());
    String role = hasTaskRole ? task.getContent() : request.role();
    log.info("MonitorChat context built userId={} taskId={} contextSize={} roleSource={}",
        principal.subjectId(), request.taskId(), context == null ? 0 : context.length(),
        hasTaskRole ? "TASK" : "REQUEST");

    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("query", request.message());
    ObjectNode inputs = payload.putObject("inputs");
    inputs.put("context", context == null ? "" : context);
    if (StringUtils.hasText(role)) {
      inputs.put("user_custom_role", role);
    }
    payload.put("user", "user-" + principal.subjectId());
    String mappedConversationId = null;
    if (hasContact) {
      mappedConversationId = contactConversationMappingService.getConversationId(
          principal.subjectId(), request.taskId(), request.wechatContact());
    }
    if (StringUtils.hasText(mappedConversationId)) {
      payload.put("conversation_id", mappedConversationId);
    } else if (StringUtils.hasText(request.conversationId())) {
      payload.put("conversation_id", request.conversationId());
    }

    DifyClient.DifyChatResult result;
    try {
      result = difyClient.chatMessages(payload.toString());
    } catch (TransitException ex) {
      if (isInvalidConversation(ex) && payload.hasNonNull("conversation_id")) {
        payload.remove("conversation_id");
        log.info("MonitorChat retry without conversation userId={} taskId={}", principal.subjectId(), request.taskId());
        result = difyClient.chatMessages(payload.toString());
      } else {
        throw ex;
      }
    }
    if (result.conversationId() != null && !result.conversationId().isBlank()) {
      mappingService.recordConversation(principal.subjectId(), result.conversationId());
      if (hasContact) {
        contactConversationMappingService.upsertConversationId(
            principal.subjectId(), request.taskId(), request.wechatContact(), result.conversationId());
      }
    }
    log.info("MonitorChat done userId={} taskId={} conversationId={} answerSize={}",
        principal.subjectId(), request.taskId(), result.conversationId(),
        result.answer() == null ? 0 : result.answer().length());
    return result.rawJson();
  }

  @GetMapping("/tasks/{taskId}/kb")
  public Map<String, String> getKnowledgeBase(@PathVariable("taskId") Long taskId) {
    TransitPrincipal principal = currentPrincipal();
    TaskEntity task = taskService.getById(principal.subjectId(), taskId);
    String kbId = task.getKnowledgeBaseId();
    return Map.of("knowledgeBaseId", kbId == null ? "" : kbId);
  }

  @PutMapping("/tasks/{taskId}/kb")
  public void bindKnowledgeBase(@PathVariable("taskId") Long taskId, @RequestBody BindKnowledgeBaseRequest request) {
    TransitPrincipal principal = currentPrincipal();
    taskService.bindKnowledgeBase(principal.subjectId(), taskId, request.knowledgeBaseId());
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

  @PostMapping(
      value = "/tasks/{taskId}/kb/document/create-by-file",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public UploadKnowledgeBaseDocumentResponse uploadDocumentToTaskKnowledgeBase(
      @PathVariable("taskId") Long taskId,
      @RequestPart("data") String data,
      @RequestPart("file") MultipartFile file) throws IOException {
    TransitPrincipal principal = currentPrincipal();
    TaskEntity task = taskService.getById(principal.subjectId(), taskId);
    String knowledgeBaseId = ensureTaskKnowledgeBase(task);
    String difyResponseJson = difyClient.uploadDocumentByFile(knowledgeBaseId, data, file);
    return new UploadKnowledgeBaseDocumentResponse(knowledgeBaseId, difyResponseJson);
  }

  private String adjustChatRequest(TransitPrincipal principal, String requestJson) throws IOException {
    JsonNode node = objectMapper.readTree(requestJson);
    if (!(node instanceof ObjectNode obj)) {
      return requestJson;
    }

    if (!obj.hasNonNull("user")) {
      obj.put("user", "user-" + principal.subjectId());
      log.info("Dify chatMessages set default user userId={}", principal.subjectId());
    }

    if (!obj.hasNonNull("conversation_id")) {
      String latest = mappingService.getLatestConversationId(principal.subjectId());
      if (latest != null && !latest.isBlank()) {
        obj.put("conversation_id", latest);
        log.info("Dify chatMessages set latest conversation userId={} conversationId={}",
            principal.subjectId(), latest);
      }
    }
    return objectMapper.writeValueAsString(obj);
  }

  private boolean isInvalidConversation(TransitException ex) {
    String message = ex.getMessage();
    if (message == null) {
      return false;
    }
    String lower = message.toLowerCase();
    return lower.contains("conversation not exists")
        || lower.contains("conversation not exist")
        || lower.contains("conversation not found");
  }

  private boolean hasConversationId(String requestJson) {
    try {
      JsonNode node = objectMapper.readTree(requestJson);
      return node.hasNonNull("conversation_id");
    } catch (Exception ex) {
      return false;
    }
  }

  private String stripConversationId(String requestJson) {
    try {
      JsonNode node = objectMapper.readTree(requestJson);
      if (node instanceof ObjectNode obj) {
        obj.remove("conversation_id");
        return objectMapper.writeValueAsString(obj);
      }
      return requestJson;
    } catch (Exception ex) {
      return requestJson;
    }
  }

  private String buildContextFromRetrieve(String retrieveJson) {
    if (!StringUtils.hasText(retrieveJson)) {
      return "";
    }
    try {
      JsonNode node = objectMapper.readTree(retrieveJson);
      JsonNode records = node.path("records");
      if (!records.isArray()) {
        records = node.path("data");
      }
      if (!records.isArray()) {
        records = node.path("documents");
      }
      if (!records.isArray()) {
        return "";
      }
      List<String> segments = new ArrayList<>();
      for (JsonNode record : records) {
        String text = extractRecordText(record);
        if (StringUtils.hasText(text)) {
          segments.add(text.trim());
        }
        if (segments.size() >= 4) {
          break;
        }
      }
      String context = String.join("\n\n", segments);
      log.info("MonitorChat context segmentCount={} contextSize={}", segments.size(),
          context.length());
      return context;
    } catch (Exception ex) {
      log.info("MonitorChat context build failed error={}", ex.getMessage());
      return "";
    }
  }

  private String extractRecordText(JsonNode record) {
    String[] paths = new String[] {
        "/segment/content",
        "/segment/text",
        "/content",
        "/text",
        "/document/content",
        "/document/text"
    };
    for (String path : paths) {
      JsonNode value = record.at(path);
      if (value != null && value.isTextual() && StringUtils.hasText(value.asText())) {
        return value.asText();
      }
    }
    return "";
  }

  private String buildPrompt(String role, String context, String message) {
    StringBuilder builder = new StringBuilder();
    if (StringUtils.hasText(role)) {
      builder.append("角色设定:\n").append(role.trim()).append("\n\n");
    }
    if (StringUtils.hasText(context)) {
      builder.append("知识库内容:\n").append(context.trim()).append("\n\n");
    }
    if (StringUtils.hasText(message)) {
      builder.append("对方消息:\n").append(message.trim());
    }
    return builder.toString();
  }

  private TransitPrincipal currentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return (TransitPrincipal) authentication.getPrincipal();
  }

  private String ensureTaskKnowledgeBase(TaskEntity task) {
    String existing = task.getKnowledgeBaseId();
    if (existing != null && !existing.isBlank()) {
      log.info("KnowledgeBase reuse taskId={} datasetId={}", task.getId(), existing);
      return existing;
    }

    String datasetName = buildDatasetName(task);
    DifyClient.DifyDatasetResult created = difyClient.createDataset(datasetName);
    String datasetId = created.datasetId();
    if (datasetId == null || datasetId.isBlank()) {
      log.info("KnowledgeBase create failed taskId={} datasetName={}", task.getId(), datasetName);
      throw new IllegalStateException("Failed to create knowledge base");
    }
    taskService.bindKnowledgeBase(task.getUserId(), task.getId(), datasetId);
    log.info("KnowledgeBase created taskId={} datasetId={}", task.getId(), datasetId);
    return datasetId;
  }

  private String buildDatasetName(TaskEntity task) {
    String name = task.getName();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Task name required");
    }
    if (name.length() > 15) {
      throw new IllegalArgumentException("Task name length must be <= 15");
    }
    return task.getId() + "_" + name;
  }

  public record BindKnowledgeBaseRequest(String knowledgeBaseId) {
  }

  public record UploadKnowledgeBaseDocumentResponse(String knowledgeBaseId, String difyResponseJson) {
  }

  public record MonitorChatRequest(Long taskId, String message, String role, String conversationId, String wechatContact) {
  }
}
