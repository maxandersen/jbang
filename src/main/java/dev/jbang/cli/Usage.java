package dev.jbang.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.jbang.cli.usage.UsageSpecGenerator;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

@Command(name = "usage", description = "Generate a Usage (usage.jdx.dev) KDL spec for the jbang CLI.")
public class Usage extends BaseCommand {

	@Option(names = "--out", paramLabel = "<file>", description = "Write the spec to the given file (defaults to stdout)")
	Path output;

	@Option(names = "--include-hidden", description = "Include commands and options marked as hidden")
	boolean includeHidden;

	@Option(names = "--root", paramLabel = "<command>", description = "Limit the export to a specific subcommand path (e.g. 'catalog add')")
	String rootPath;

	@Override
	public Integer doCall() throws IOException {
		CommandLine topLevel = topLevel(spec.commandLine());
		CommandLine target = resolveTarget(topLevel, rootPath);
		UsageSpecGenerator generator = new UsageSpecGenerator();
		UsageSpecGenerator.Options options = new UsageSpecGenerator.Options(includeHidden);
		String result = generator.generate(target, options);
		write(result);
		return EXIT_OK;
	}

	private CommandLine topLevel(CommandLine commandLine) {
		CommandLine current = commandLine;
		while (current.getParent() != null) {
			current = current.getParent();
		}
		return current;
	}

	private CommandLine resolveTarget(CommandLine root, String path) {
		if (path == null || path.trim().isEmpty()) {
			return root;
		}
		List<String> segments = new ArrayList<>(Arrays.asList(path.trim().split("\\s+")));
		if (!segments.isEmpty() && segments.get(0).equalsIgnoreCase(root.getCommandSpec().name())) {
			segments.remove(0);
		}
		CommandLine current = root;
		for (String segment : segments) {
			current = findSubcommand(current, segment)
				.orElseThrow(() -> new ExitException(EXIT_INVALID_INPUT,
						"Unknown subcommand '" + segment + "' in path '" + path + "'"));
		}
		return current;
	}

	private Optional<CommandLine> findSubcommand(CommandLine parent, String name) {
		Map<String, CommandLine> unique = new LinkedHashMap<>();
		for (CommandLine sub : parent.getSubcommands().values()) {
			unique.putIfAbsent(sub.getCommandSpec().name(), sub);
		}
		for (CommandLine sub : unique.values()) {
			CommandSpec subSpec = sub.getCommandSpec();
			if (!includeHidden && subSpec.usageMessage().hidden()) {
				continue;
			}
			if (subSpec.name().equals(name)) {
				return Optional.of(sub);
			}
			for (String alias : subSpec.aliases()) {
				if (alias.equals(name)) {
					return Optional.of(sub);
				}
			}
		}
		return Optional.empty();
	}

	private void write(String contents) throws IOException {
		if (output == null) {
			realOut.print(contents);
			if (!contents.endsWith(System.lineSeparator())) {
				realOut.println();
			}
			realOut.flush();
			return;
		}
		Path target = output.toAbsolutePath();
		Path parent = target.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		Files.write(target, contents.getBytes(StandardCharsets.UTF_8));
	}
}
