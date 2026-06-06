package com.pkgfit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RegistryServiceTest {

    private RegistryService registryService;
    private RestClient restClient;
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.defaultHeader(anyString(), anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), (Object) any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);

        registryService = new RegistryService(builder);
    }

    @Test
    void fetchPackageMetadataShouldReturnJsonNodeForSimpleName() throws Exception {
        String json = "{\"name\":\"foo\",\"versions\":{\"1.0.0\":{}}}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expected = mapper.readTree(json);

        when(responseSpec.body(JsonNode.class)).thenReturn(expected);

        JsonNode result = registryService.fetchPackageMetadata("foo");

        assertNotNull(result);
        assertEquals("foo", result.get("name").asText());
    }

    @Test
    void fetchPackageMetadataShouldReturnNullOnError() {
        when(responseSpec.body(JsonNode.class)).thenThrow(new RuntimeException("Connection refused"));

        JsonNode result = registryService.fetchPackageMetadata("foo");

        assertNull(result);
    }

    @Test
    void fetchPackageMetadataShouldReturnNullOnEmptyResponse() {
        when(responseSpec.body(JsonNode.class)).thenReturn(null);

        JsonNode result = registryService.fetchPackageMetadata("foo");

        assertNull(result);
    }

    @Test
    void fetchPackageMetadataShouldHandleScopedPackageName() throws Exception {
        String json = "{\"name\":\"@scope/foo\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expected = mapper.readTree(json);

        when(responseSpec.body(JsonNode.class)).thenReturn(expected);

        // The encoded name @scope%2Ffoo should be used in the URI
        when(requestHeadersUriSpec.uri("/{packageName}", "@scope%2Ffoo"))
                .thenReturn(requestHeadersUriSpec);

        JsonNode result = registryService.fetchPackageMetadata("@scope/foo");

        assertNotNull(result);
        assertEquals("@scope/foo", result.get("name").asText());
    }
}
