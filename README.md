# MCP Example

This repo shows how-to create:

- an MCP Server in Java using WildFly
- an MCP Client in Java using WildFly

This repo it is, basically, a simpler version of [langchain4j-cdi-mcp](https://github.com/langchain4j/langchain4j-cdi/blob/main/langchain4j-cdi-mcp/README.md);

## Intro

Both client and server projects were created with the [wildfly-jakartaee-webapp-archetype](https://github.com/wildfly/wildfly-archetypes/tree/main/wildfly-jakartaee-webapp-archetype):

```bash
mvn archetype:generate -DgroupId=org.wildfly.mcp.example -DartifactId=mcp-server -Dversion=1.0-SNAPSHOT -DarchetypeGroupId=org.wildfly.archetype -DarchetypeArtifactId=wildfly-jakartaee-webapp-archetype -DarchetypeVersion=42.0.0.Final -s ~/.m2/settings-empty.xml
mvn archetype:generate -DgroupId=org.wildfly.mcp.example -DartifactId=mcp-client -Dversion=1.0-SNAPSHOT -DarchetypeGroupId=org.wildfly.archetype -DarchetypeArtifactId=wildfly-jakartaee-webapp-archetype -DarchetypeVersion=42.0.0.Final -s ~/.m2/settings-empty.xml
```

## LangChain4J-CDI

Both client and server projects rely on [](https://github.com/langchain4j/langchain4j-cdi) for AI:

```xml
<dependency>
    <groupId>dev.langchain4j.cdi.mcp</groupId>
    <artifactId>langchain4j-cdi-mcp-server</artifactId>
    <version>1.5.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j.cdi.mcp</groupId>
    <artifactId>langchain4j-cdi-mcp-portable-ext</artifactId>
    <version>1.5.0</version>
</dependency>
```

## Local LLM

We use [](https://docs.ollama.com/docker) to runa simple LLM locally with CPU only hardware:

```bash
podman run -it --rm -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
```

```bash
podman exec -it ollama ollama run qwen3.5:4b
```

## MCP Server

Exposing any [CDI Bean](https://jakarta.ee/specifications/cdi/) you might already have developed, is as easy as adding the `@Tool` annotation:

```java
package org.wildfly.mcp.example;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;

@ApplicationScoped
public class WeatherService {
    @Tool(description = "Get the current weather for a city")
    public String getWeather(
            @ToolArg(description = "City name") String city,
            @ToolArg(description = "Unit: celsius or fahrenheit", required = false) String unit) {
        // your real implementation here
        return "Sunny, 22°C in " + city;
    }
}
```

### Build MCP Server

```bash
mvn clean install -DskipTests -s ~/.m2/settings-empty.xml
```

### Run MCP Server

```bash
java -jar target/mcp-server-bootable.jar

...
16:11:19,037 INFO  [dev.langchain4j.cdi.mcp.portableextension.McpServerPortableExtension] (MSC service thread 1-3) MCP: Detected MCP annotations in org.wildfly.mcp.example.WeatherService
16:11:19,140 INFO  [dev.langchain4j.cdi.mcp.portableextension.McpServerPortableExtension] (MSC service thread 1-3) MCP: Registered tool 'getWeather' from WeatherService
16:11:19,144 INFO  [dev.langchain4j.cdi.mcp.portableextension.McpServerPortableExtension] (MSC service thread 1-3) MCP: Registered 1 tool(s), 0 resource(s), 0 prompt(s) total
...
```

these log lines prove the MCP was correctly detected;

### Test MCP Server

Now, let's verify MCP tools were correctly exposed:

```bash
curl -s -X POST http://localhost:8080/mcp-server/mcp   -H "Content-Type: application/json"   -H "MCP-Protocol-Version: 2026-07-28"   -H "Mcp-Method: tools/list"   -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{"_meta":{
        "io.modelcontextprotocol/protocolVersion":"2026-07-28",
        "io.modelcontextprotocol/clientInfo":{"name":"curl","version":"1.0"},
        "io.modelcontextprotocol/clientCapabilities":{}}}}'
{
   "jsonrpc":"2.0",
   "id":1,
   "result":{
      "tools":[
         {
            "description":"Get the current weather for a city",
            "inputSchema":{
               "type":"object",
               "properties":{
                  "arg0":{
                     "type":"string",
                     "description":"City name"
                  },
                  "arg1":{
                     "type":"string",
                     "description":"Unit: celsius or fahrenheit"
                  }
               },
               "required":[
                  "arg0"
               ]
            },
            "name":"getWeather"
         }
      ],
      "ttlMs":0,
      "cacheScope":"public",
      "resultType":"complete",
      "_meta":{
         "io.modelcontextprotocol/serverInfo":{
            "name":"langchain4j-cdi",
            "version":"unknown"
         }
      }
   }
}
```

the JSON payload contains the MCP description of MCP tools exposed by this MCP endpoint;

## MCP Client

`WeatherMcpProducer` creates a remote MCP client and exposes its `getWeather` tool to `WeatherAIService` through LangChain4j CDI. The MCP endpoint is configured with `mcp.server.url` in `microprofile-config.properties`.

### Build both applications

From the repository root:

```bash
mvn clean package
```

### Run the client

Keep Ollama and the MCP server running, then start the client on port 8081 so it does not conflict with the server:

```bash
cd mcp-client
java -Djboss.socket.binding.port-offset=1 -jar target/mcp-client-bootable.jar
```

Ask for the weather through the client:

```bash
curl --get --data-urlencode 'city=Rome' http://localhost:8081/mcp-client/weather
```

The model should call the remote `getWeather` MCP tool and return its result. A missing or blank `city` returns HTTP 400.

To use a different MCP endpoint, add `-Dmcp.server.url=http://host:port/mcp` to the Java command.
