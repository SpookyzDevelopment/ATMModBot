package org.spookydevz.chatbridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PterodactylClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(PterodactylClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient httpClient;
    private final String apiUrl;
    private final String apiKey;
    private final String serverId;
    private final Gson gson;
    
    public PterodactylClient(String apiUrl, String apiKey, String serverId) {
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.apiKey = apiKey;
        this.serverId = serverId;
        this.gson = new Gson();
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public CompletableFuture<Boolean> sendCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject commandJson = new JsonObject();
                commandJson.addProperty("command", command);
                
                RequestBody body = RequestBody.create(gson.toJson(commandJson), JSON);
                
                Request request = new Request.Builder()
                        .url(apiUrl + "/api/client/servers/" + serverId + "/command")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .post(body)
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        LOGGER.debug("Successfully sent command to Pterodactyl: {}", command);
                        return true;
                    } else {
                        LOGGER.error("Failed to send command to Pterodactyl. Status: {}, Body: {}", 
                                response.code(), response.body() != null ? response.body().string() : "null");
                        return false;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error sending command to Pterodactyl", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<JsonObject> getServerStatus() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(apiUrl + "/api/client/servers/" + serverId + "/resources")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Accept", "application/json")
                        .get()
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        return gson.fromJson(responseBody, JsonObject.class);
                    } else {
                        LOGGER.error("Failed to get server status from Pterodactyl. Status: {}", response.code());
                        return null;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error getting server status from Pterodactyl", e);
                return null;
            }
        });
    }
    
    public CompletableFuture<Boolean> setPowerState(String action) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject powerJson = new JsonObject();
                powerJson.addProperty("signal", action); // start, stop, restart, kill
                
                RequestBody body = RequestBody.create(gson.toJson(powerJson), JSON);
                
                Request request = new Request.Builder()
                        .url(apiUrl + "/api/client/servers/" + serverId + "/power")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .post(body)
                        .build();
                
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        LOGGER.info("Successfully sent power action '{}' to Pterodactyl", action);
                        return true;
                    } else {
                        LOGGER.error("Failed to send power action to Pterodactyl. Status: {}, Body: {}", 
                                response.code(), response.body() != null ? response.body().string() : "null");
                        return false;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error sending power action to Pterodactyl", e);
                return false;
            }
        });
    }
    
    public void shutdown() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}