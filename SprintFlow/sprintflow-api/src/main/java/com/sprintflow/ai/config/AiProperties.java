package com.sprintflow.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sprintflow.ai")
public class AiProperties {

    private boolean enabled = true;
    private String provider = "GROQ";
    private String apiKey = "";
    private String baseUrl = "https://api.groq.com/openai/v1";
    private String model = "openai/gpt-oss-20b";
    private int timeoutSeconds = 25;
    private int maxInputCharacters = 600;
    private int maxOutputCharacters = 5000;
    private int maxConcurrentRequests = 2;
    private int maxCompletionTokens = 1200;
    private double temperature = 0.2;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxInputCharacters() { return maxInputCharacters; }
    public void setMaxInputCharacters(int maxInputCharacters) { this.maxInputCharacters = maxInputCharacters; }
    public int getMaxOutputCharacters() { return maxOutputCharacters; }
    public void setMaxOutputCharacters(int maxOutputCharacters) { this.maxOutputCharacters = maxOutputCharacters; }
    public int getMaxConcurrentRequests() { return maxConcurrentRequests; }
    public void setMaxConcurrentRequests(int maxConcurrentRequests) { this.maxConcurrentRequests = maxConcurrentRequests; }
    public int getMaxCompletionTokens() { return maxCompletionTokens; }
    public void setMaxCompletionTokens(int maxCompletionTokens) { this.maxCompletionTokens = maxCompletionTokens; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}
