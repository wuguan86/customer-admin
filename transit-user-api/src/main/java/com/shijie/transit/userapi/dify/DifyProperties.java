package com.shijie.transit.userapi.dify;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dify")
public class DifyProperties {
  private String baseUrl;
  private String chatApiKey;
  private String datasetApiKey;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getChatApiKey() {
    return chatApiKey;
  }

  public void setChatApiKey(String chatApiKey) {
    this.chatApiKey = chatApiKey;
  }

  public String getDatasetApiKey() {
    return datasetApiKey;
  }

  public void setDatasetApiKey(String datasetApiKey) {
    this.datasetApiKey = datasetApiKey;
  }
}
