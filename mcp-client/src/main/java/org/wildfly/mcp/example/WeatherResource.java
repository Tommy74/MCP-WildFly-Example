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

@ApplicationScoped
@Path("/weather")
public class WeatherResource {

    private static final Logger LOGGER = Logger.getLogger(WeatherResource.class.getName());

    @Inject
    WeatherAIService aiService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response chatWithAssistant(@QueryParam("city") String city) {
        if (city == null || city.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("The 'city' query parameter is required and must not be blank.")
                    .build();
        }

        String requestedCity = city.trim();
        try {
            return Response.ok(aiService.chat(requestedCity)).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while getting weather for city: " + requestedCity, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("The weather service is temporarily unavailable. Please try again later.")
                    .build();
        }
    }
}
