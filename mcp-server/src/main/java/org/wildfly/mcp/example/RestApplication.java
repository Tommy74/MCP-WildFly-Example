package org.wildfly.mcp.example;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activates Jakarta REST so the MCP endpoint ({@code @Path("/mcp")}) shipped in
 * langchain4j-cdi-mcp-server is exposed at {@code http://localhost:8080/mcp}.
 */
@ApplicationPath("/")
public class RestApplication extends Application {
}