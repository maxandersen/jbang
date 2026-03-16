package dev.jbang.cli.usage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import dev.jbang.util.Util;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ScopeType;

/**
 * Converts a picocli {@link CommandLine} definition into a Usage
 * (usage.jdx.dev) KDL specification.
 */
public class UsageSpecGenerator {

	public static final class Options {
		private final boolean includeHidden;

		public Options(boolean includeHidden) {
			this.includeHidden = includeHidden;
		}

		public boolean includeHidden() {
			return includeHidden;
		}
	}

	public String generate(CommandLine commandLine, Options options) {
		Writer writer = new Writer(options);
		writer.writeRoot(commandLine);
		return writer.toString();
	}

	private static final class Writer {
		private final Options options;
		private final StringBuilder out = new StringBuilder();

		Writer(Options options) {
			this.options = options;
		}

		void writeRoot(CommandLine commandLine) {
			CommandSpec spec = commandLine.getCommandSpec();
			writeMetadata(spec);
			out.append('\n');
			boolean wroteOptions = writeOptions(spec, 0);
			boolean wrotePositionals = writePositionals(spec, 0);
			boolean wroteCommands = writeSubcommands(commandLine, 0);
			if (!wroteOptions && !wrotePositionals && !wroteCommands) {
				// remove extra newline if nothing else was written
				trimTrailingNewline();
			}
		}

		private void trimTrailingNewline() {
			if (out.length() > 0 && out.charAt(out.length() - 1) == '\n') {
				out.deleteCharAt(out.length() - 1);
			}
		}

		private void writeMetadata(CommandSpec spec) {
			String friendlyName = firstNonBlank(spec.name(), spec.qualifiedName(" "));
			String bin = commandPath(spec);
			String about = description(spec);
			appendLine(0, "name " + quoted(friendlyName));
			appendLine(0, "bin " + quoted(bin));
			if (!about.isEmpty()) {
				appendLine(0, "about " + quoted(about));
			}
			String version = Util.getJBangVersion();
			if (!Util.isNullOrEmptyString(version)) {
				appendLine(0, "version " + quoted(version));
			}
			List<String> aliases = commandAliases(spec);
			for (String alias : aliases) {
				appendLine(0, "alias " + quoted(alias));
			}
		}

		private boolean writeOptions(CommandSpec spec, int indent) {
			List<OptionSpec> options = spec.options()
				.stream()
				.filter(opt -> this.options.includeHidden() || !opt.hidden())
				.sorted((a, b) -> primaryName(a).compareTo(primaryName(b)))
				.collect(Collectors.toList());
			for (OptionSpec option : options) {
				appendLine(indent, formatFlag(option));
			}
			return !options.isEmpty();
		}

		private String formatFlag(OptionSpec option) {
			StringBuilder line = new StringBuilder();
			line.append("flag ");
			line.append(quoted(flagLabel(option)));
			String help = description(option);
			if (!help.isEmpty()) {
				line.append(" help=").append(quoted(help));
			}
			if (option.required()) {
				line.append(" required=#true");
			}
			if (option.scopeType() == ScopeType.INHERIT) {
				line.append(" global=#true");
			}
			if (isCountable(option)) {
				line.append(" count=#true");
			}
			String defaultValue = option.defaultValue();
			if (defaultValue != null) {
				line.append(" default=").append(quoted(defaultValue));
			}
			if (option.hidden()) {
				line.append(" hide=#true");
			}
			return line.toString();
		}

		private boolean writePositionals(CommandSpec spec, int indent) {
			List<PositionalParamSpec> params = spec.positionalParameters()
				.stream()
				.filter(param -> this.options.includeHidden() || !param.hidden())
				.sorted((a, b) -> Integer.compare(a.index().min, b.index().min))
				.collect(Collectors.toList());
			for (PositionalParamSpec param : params) {
				appendLine(indent, formatArg(param));
			}
			return !params.isEmpty();
		}

		private String formatArg(PositionalParamSpec param) {
			StringBuilder line = new StringBuilder();
			line.append("arg ");
			line.append(quoted(argLabel(param)));
			String help = description(param);
			if (!help.isEmpty()) {
				line.append(" help=").append(quoted(help));
			}
			if (param.required()) {
				line.append(" required=#true");
			}
			if (isCountable(param)) {
				line.append(" repeat=#true");
			}
			String defaultValue = param.defaultValue();
			if (defaultValue != null) {
				line.append(" default=").append(quoted(defaultValue));
			}
			if (param.hidden()) {
				line.append(" hide=#true");
			}
			return line.toString();
		}

		private boolean writeSubcommands(CommandLine commandLine, int indent) {
			List<CommandLine> subcommands = collectSubcommands(commandLine);
			for (CommandLine sub : subcommands) {
				writeCommand(sub, indent);
			}
			return !subcommands.isEmpty();
		}

		private void writeCommand(CommandLine commandLine, int indent) {
			CommandSpec spec = commandLine.getCommandSpec();
			StringBuilder header = new StringBuilder();
			header.append("cmd ");
			header.append(quoted(spec.name()));
			String help = description(spec);
			if (!help.isEmpty()) {
				header.append(" help=").append(quoted(help));
			}
			appendLine(indent, header.append(" {").toString());
			List<String> aliases = commandAliases(spec);
			for (String alias : aliases) {
				appendLine(indent + 1, "alias " + quoted(alias));
			}
			writeOptions(spec, indent + 1);
			writePositionals(spec, indent + 1);
			writeSubcommands(commandLine, indent + 1);
			appendLine(indent, "}");
		}

		private List<CommandLine> collectSubcommands(CommandLine commandLine) {
			Map<String, CommandLine> uniqueByName = new TreeMap<>();
			for (CommandLine sub : commandLine.getSubcommands().values()) {
				CommandSpec subSpec = sub.getCommandSpec();
				if (!options.includeHidden() && subSpec.usageMessage().hidden()) {
					continue;
				}
				uniqueByName.putIfAbsent(subSpec.name(), sub);
			}
			return new ArrayList<>(uniqueByName.values());
		}

		private String flagLabel(OptionSpec option) {
			String joinedNames = String.join(" ", option.names());
			if (option.arity().max > 0) {
				String paramLabel = paramLabel(option.paramLabel(), option.arity().min > 0);
				return String.join(" ", joinedNames, paramLabel).trim();
			}
			return joinedNames;
		}

		private String argLabel(PositionalParamSpec param) {
			String label = param.paramLabel();
			return paramLabel(label, param.required());
		}

		private String paramLabel(String rawLabel, boolean required) {
			String value = Objects.toString(rawLabel, "value").trim();
			if (value.isEmpty()) {
				value = "value";
			}
			boolean wrapped = value.startsWith("<") || value.startsWith("[");
			if (wrapped) {
				return value;
			}
			return required ? "<" + value + ">" : "[" + value + "]";
		}

		private boolean isCountable(OptionSpec option) {
			return option.isMultiValue() || option.arity().max > 1 || option.arity().max == Integer.MAX_VALUE;
		}

		private boolean isCountable(PositionalParamSpec param) {
			return param.isMultiValue() || param.arity().max > 1 || param.arity().max == Integer.MAX_VALUE;
		}

		private String primaryName(OptionSpec option) {
			String[] names = option.names();
			return names.length == 0 ? option.longestName() : names[0];
		}

		private List<String> commandAliases(CommandSpec spec) {
			List<String> names = new ArrayList<>(spec.names());
			if (names.isEmpty()) {
				return Collections.emptyList();
			}
			return names.subList(1, names.size());
		}

		private String description(OptionSpec option) {
			return join(option.description());
		}

		private String description(PositionalParamSpec param) {
			return join(param.description());
		}

		private String description(CommandSpec spec) {
			String desc = join(spec.usageMessage().description());
			if (!desc.isEmpty()) {
				return desc;
			}
			return join(spec.usageMessage().header());
		}

		private String join(String[] values) {
			if (values == null || values.length == 0) {
				return "";
			}
			return Arrays.stream(values)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.joining(" "));
		}

		private void appendLine(int indent, String line) {
			if (indent > 0) {
				for (int i = 0; i < indent; i++) {
					out.append("  ");
				}
			}
			out.append(line);
			out.append('\n');
		}

		private String quoted(String value) {
			return "\"" + escape(value) + "\"";
		}

		private String escape(String value) {
			return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n");
		}

		private String commandPath(CommandSpec spec) {
			Deque<String> names = new ArrayDeque<>();
			CommandSpec current = spec;
			while (current != null && current.name() != null && !current.name().isEmpty()) {
				names.addFirst(current.name());
				current = current.parent();
			}
			return String.join(" ", names);
		}

		private String firstNonBlank(String... values) {
			if (values == null) {
				return "";
			}
			for (String value : values) {
				if (hasText(value)) {
					return value;
				}
			}
			return "";
		}

		private boolean hasText(String value) {
			return value != null && !value.trim().isEmpty();
		}

		@Override
		public String toString() {
			return out.toString();
		}
	}
}
