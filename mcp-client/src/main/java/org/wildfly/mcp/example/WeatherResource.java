package org.wildfly.mcp.example;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static dev.langchain4j.cdi.plugin.CommonLLMPluginCreator.LOGGER;

@ApplicationScoped
@Path("/weather")
public class WeatherResource {

    @Inject
    WeatherAIService aiService;

    @Inject
    @ConfigProperty(name = "mcp.server.url")
    String mcpServerUrl;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithAssistant(@QueryParam("city") String city) {
        try {
            return Response.ok(aiService.chat(city))
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while getting weather for city: " + city, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("My failure reason is:\n\n" + e.getMessage())
                    .build();
        }
    }
}
