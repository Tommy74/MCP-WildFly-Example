package org.wildfly.mcp.example;

import java.time.temporal.ChronoUnit;

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.service.SystemMessage;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

@RegisterAIService(
        // this is the qwen3 LLM served by ollama
        chatModelName = "chat-model",
        // these are tools served by the mcp-server project
        toolProviderName = "mcp-tools",
        scope = ApplicationScoped.class
)
public interface WeatherAIService {

    @SystemMessage("""
            You are a weather assistant. For every city request, use the getWeather tool
            provided by the configured MCP server. Pass the requested city and use celsius
            unless the user explicitly asks for fahrenheit. Answer concisely using the tool
            result, and never invent weather data when the tool is unavailable.
            """)
    @Timeout(unit = ChronoUnit.MINUTES, value = 5)
    @Retry(maxRetries = 2)
    @Fallback(fallbackMethod = "fallback")
    String chat(String city);

    default String fallback(String city) {
        return "Sorry, I'm having trouble processing your request at the moment. Please try again later.";
    }
}
