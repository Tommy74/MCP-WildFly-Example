package org.wildfly.mcp.example;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Activates Jakarta REST for the MCP client's weather endpoint. */
@ApplicationPath("/")
public class RestApplication extends Application {
}
