package org.wildfly.mcp.example;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class WeatherMcpProducer {

    @Produces
    @Named("mcp-client")
    @ApplicationScoped
    McpClient mcpClient(@ConfigProperty(name = "mcp.server.url") String serverUrl) {
        McpTransport transport = StreamableHttpMcpTransport.builder()
                .url(serverUrl)
                .build();

        return DefaultMcpClient.builder()
                .clientName("wildfly-weather-client")
                .protocolVersion("2026-07-28")
                .transport(transport)
                .build();
    }

    @Produces
    @Named("mcp-tools")
    @ApplicationScoped
    ToolProvider mcpTools(@Named("mcp-client") McpClient client) {
        return McpToolProvider.builder()
                .mcpClients(client)
                .filterToolNames("getWeather")
                .failIfOneServerFails(true)
                .build();
    }

    void closeMcpClient(@Disposes @Named("mcp-client") McpClient client) throws Exception {
        client.close();
    }
}
