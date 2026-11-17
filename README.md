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

## LangChain4j and LangChain4j-CDI

This example uses two related projects:

- [LangChain4j](https://github.com/langchain4j/langchain4j): the core Java library for LLM applications. It provides chat model integrations (e.g. Ollama), AI services and the MCP client (`langchain4j-mcp`)
- [LangChain4j-CDI](https://github.com/langchain4j/langchain4j-cdi): plugs LangChain4j into Jakarta EE / CDI. AI services become CDI beans (`@RegisterAIService`), models are configured with MicroProfile Config, and MicroProfile Fault Tolerance annotations work on AI services. It also provides an MCP server that exposes CDI beans as MCP tools

| Project | LangChain4j | LangChain4j-CDI |
|---------|-------------|-----------------|
| `mcp-server` | not used directly | MCP server: `langchain4j-cdi-mcp-server`, `langchain4j-cdi-mcp-portable-ext` |
| `mcp-client` | `langchain4j-mcp` (MCP client), `langchain4j-ollama` (chat model) | AI service integration: `langchain4j-cdi-core`, `langchain4j-cdi-portable-ext`, `langchain4j-cdi-config`, `langchain4j-cdi-fault-tolerance` |

### MCP server

The server only needs LangChain4j-CDI. Its MCP modules publish the `/mcp` endpoint (Streamable HTTP over Jakarta REST) and register every CDI bean method annotated with `@Tool` as an MCP tool. The `@Tool`/`@ToolArg` annotations come from `org.mcpjava:mcp-server-api`, which these modules pull in:

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

### MCP client

The client uses both projects:

- LangChain4j provides the MCP client (`DefaultMcpClient`, `StreamableHttpMcpTransport` and `McpToolProvider`, used in `WeatherMcpProducer`) and the Ollama chat model. Versions come from `langchain4j-bom`; `langchain4j-mcp` is still a beta, so its version is set explicitly
- LangChain4j-CDI turns the `WeatherAIService` interface into a CDI bean (`@RegisterAIService`), builds the chat model from `microprofile-config.properties`, and applies `@Retry`/`@Timeout`/`@Fallback` to it

```xml
<!-- LangChain4j -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-mcp</artifactId>
    <version>1.23.0-beta33</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId> <!-- version from langchain4j-bom 1.23.0 -->
</dependency>

<!-- LangChain4j-CDI (version 1.5.1) -->
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-core</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j.cdi</groupId>
    <artifactId>langchain4j-cdi-portable-ext</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j.cdi.mp</groupId>
    <artifactId>langchain4j-cdi-config</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j.cdi.mp</groupId>
    <artifactId>langchain4j-cdi-fault-tolerance</artifactId>
</dependency>
```

## Local LLM

We use [](https://docs.ollama.com/docker) to run a simple LLM locally with CPU only hardware:

Start ollama:
```bash
podman run -it --rm -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
```

Download a model:
```bash
podman exec -it ollama ollama pull qwen3.5:4b
```

Check the model is being served: 
```bash
curl http://localhost:11434/api/chat -d '{
  "model": "qwen3.5:4b",
  "messages": [{"role": "user", "content": "Hello!"}],
  "stream": false
}'
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
mvn clean install -DskipTests -s settings-empty.xml
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
mvn clean install -DskipTests -s settings-empty.xml
```

### Run the client

Keep Ollama and the MCP server running, then start the client on port 8081 so it does not conflict with the server:

```bash
cd mcp-client
java -Djboss.socket.binding.port-offset=1 -jar target/mcp-client-bootable.jar
```

Ask for the weather through the client:

```bash
curl --get --data-urlencode 'city=Milan' http://localhost:8081/mcp-client/weather
It is currently sunny with a temperature of 22°C in Milan.
```

The model should call the remote `getWeather` MCP tool and return its result. A missing or blank `city` returns HTTP 400.

To use a different MCP endpoint, add `-Dmcp.server.url=http://host:port/mcp` to the Java command.

