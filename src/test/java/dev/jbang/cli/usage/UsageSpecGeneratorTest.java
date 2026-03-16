package dev.jbang.cli.usage;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;

class UsageSpecGeneratorTest {

	@Command(name = "root", description = "Root command", subcommands = Child.class)
	static class Root {

		@Option(names = { "-v", "--verbose" }, description = "Verbose logging", scope = ScopeType.INHERIT)
		boolean verbose;

		@Parameters(index = "0", paramLabel = "input", description = "Input file")
		String input;
	}

	@Command(name = "child", description = "Child command", aliases = { "c" })
	static class Child {

		@Option(names = "--force", description = "Force execution")
		boolean force;

		@Parameters(paramLabel = "item", description = "Optional item", arity = "0..1")
		String item;
	}

	@Test
	void generatesUsageSpec() {
		CommandLine commandLine = new CommandLine(new Root());
		UsageSpecGenerator generator = new UsageSpecGenerator();
		String output = generator.generate(commandLine, new UsageSpecGenerator.Options(false));

		assertTrue(output.contains("name \"root\""));
		assertTrue(output.contains("bin \"root\""));
		assertTrue(output.contains("flag \"-v --verbose\""));
		assertTrue(output.contains("global=#true"));
		assertTrue(output.contains("arg \"<input>\""));
		assertTrue(output.contains("cmd \"child\""));
		assertTrue(output.contains("alias \"c\""));
	}
}
