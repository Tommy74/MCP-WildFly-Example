package org.wildfly.mcp.example;

import java.time.temporal.ChronoUnit;

import dev.langchain4j.cdi.spi.RegisterAIService;
import dev.langchain4j.service.SystemMessage;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

@RegisterAIService(
        chatModelName = "chat-model",
        chatMemoryName = "chat-memory",
        tools = CarBookingService.class
)
public interface WeatherAIService {

    @SystemMessage("""
            Get the current weather for a city.

            Today is {{current_date}}.
            """)
    @Timeout(unit = ChronoUnit.MINUTES, value = 5)
    @Retry(maxRetries = 2)
    @Fallback(fallbackMethod = "fallback")
    String chat(String city);

    default String fallback(String city) {
        return "Sorry, I'm having trouble processing your request at the moment. Please try again later.";
    }
}
