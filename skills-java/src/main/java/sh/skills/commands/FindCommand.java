package sh.skills.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import sh.skills.util.Console;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Implements `skills find [query]`.
 * Searches for skills via the skills.sh API.
 * Mirrors src/find.ts from the TypeScript source.
 */
@Command(
    name = "find",
    description = "Search for skills in the skills ecosystem",
    mixinStandardHelpOptions = true
)
public class FindCommand implements Callable<Integer> {

    private static final String SKILLS_API = "https://skills.sh/api/skills";

    @Parameters(index = "0", arity = "0..1", description = "Search query")
    private String query;

    @Option(names = {"--json"}, description = "Output as JSON")
    private boolean json;

    @Option(names = {"-n", "--limit"}, description = "Maximum number of results (default: 10)", defaultValue = "10")
    private int limit;

    @Override
    public Integer call() {
        try {
            return execute();
        } catch (Exception e) {
            Console.error(e.getMessage());
            return 1;
        }
    }

    private int execute() throws Exception {
        String url = SKILLS_API;
        if (query != null && !query.isEmpty()) {
            url += "?q=" + java.net.URLEncoder.encode(query, "UTF-8");
        }
        url += (url.contains("?") ? "&" : "?") + "limit=" + limit;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(
                HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
        } catch (Exception e) {
            Console.error("Could not reach skills.sh: " + e.getMessage());
            Console.log("Check your internet connection or try browsing https://skills.sh");
            return 1;
        }

        if (response.statusCode() != 200) {
            Console.error("skills.sh API returned " + response.statusCode());
            return 1;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        if (json) {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
            return 0;
        }

        JsonNode skills = root.isArray() ? root : root.path("skills");

        if (!skills.isArray() || skills.size() == 0) {
            Console.log("No skills found" + (query != null ? " for '" + query + "'" : "") + ".");
            Console.log("Browse all skills at " + Console.cyan("https://skills.sh"));
            return 0;
        }

        Console.log(Console.bold("Skills" + (query != null ? " matching '" + query + "'" : "")) + ":\n");

        for (JsonNode skill : skills) {
            String name = skill.path("name").asText("unknown");
            String description = skill.path("description").asText("");
            String source = skill.path("source").asText("");
            int installs = skill.path("installs").asInt(0);

            Console.log(Console.bold(name));
            if (!description.isEmpty()) Console.log("  " + Console.dim(description));
            if (!source.isEmpty()) Console.log("  " + Console.cyan(source));
            if (installs > 0) Console.log("  " + Console.gray(installs + " installs"));
            Console.log("");
        }

        Console.log("To install a skill: " + Console.cyan("skills add <source>"));
        return 0;
    }
}
