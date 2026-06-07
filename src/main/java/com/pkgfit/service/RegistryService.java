package com.pkgfit.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class RegistryService {
    
    private final RestClient restClient;

    public RegistryService(RestClient.Builder restClientBuilder,
            @Value("${pkgfit.registry.url}") String registryBase,
            @Value("${pkgfit.registry.connect-timeout:5s}") Duration connectTimeout,
            @Value("${pkgfit.registry.read-timeout:10s}") Duration readTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        this.restClient = restClientBuilder
            .baseUrl(registryBase)
            .defaultHeader("Accept", "application/json")
            .requestFactory(factory)
            .build();
    }

    public JsonNode fetchPackageMetadata(String packageName){
        String encodeName = encodePackageName(packageName);
        try{
            JsonNode response = restClient.get()
                .uri("/{packageName}", encodeName)
                .retrieve()
                .body(JsonNode.class);

            if(response == null) throw new RuntimeException("Empty response from registry for package: " + packageName);
            return response;
        }catch(Exception e){
            return null;
        }
    }

    private String encodePackageName(String packageName){
        if(packageName.startsWith("@")){
            return packageName.replace("/", "%2F");
        }
        return packageName;
    }

    public static class RegistryException extends RuntimeException{
        public RegistryException(String message){
            super(message);
        }

        public RegistryException(String message, Throwable cause){
            super(message, cause);
        }
    }

    public JsonNode searchPackages(String query){
        try{
            return restClient.get()
                .uri("/-/v1/search?text={query}&size=10", query)
                .retrieve()
                .body(JsonNode.class);
        }catch(Exception e){
            return null;
        }
    }

}
