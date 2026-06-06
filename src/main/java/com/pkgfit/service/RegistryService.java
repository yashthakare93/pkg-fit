package com.pkgfit.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class RegistryService {
    private static final String REGISTRY_BASE = "https://registry.npmjs.org/";
    private final RestClient restClient;

    public RegistryService(RestClient.Builder restClientBuilder){
        this.restClient = restClientBuilder
            .baseUrl(REGISTRY_BASE)
            .defaultHeader("Accept", "application/json")
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
            System.err.println("Error fetching metadata for package " + packageName + ": " + e.getMessage());
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
            System.err.println("Error searching packages for query " + query + ": " + e.getMessage());
            return null;
        }
    }

}
