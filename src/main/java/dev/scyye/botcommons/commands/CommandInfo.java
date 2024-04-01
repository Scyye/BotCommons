package dev.scyye.botcommons.commands;

import com.google.gson.Gson;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

import static dev.scyye.botcommons.commands.CommandManager.commands;
import static dev.scyye.botcommons.commands.CommandManager.getCommand;

public class CommandInfo {

	/**
	 * The command name. Should be unique, lowercase, and not contain spaces.
	 */
	public String name;
	public String help;
	public String[] aliases;
	public Option[] args;
	public Command.Scope scope;
	public Command.Category category;
	public String permission;
	/**
	 * The command usage
	 * excludes the command name itself
	 * <p>Usage syntax:
	 * <ul>
	 *     <li>Arguments wrapped in <code>[]</code> are optional</li>
	 *     <li>Arguments wrapped in <code>&lt;&gt;</code> are required</li>
	 *     <li>Arguments separated by <code>|</code> are mutually exclusive</li>
	 *     <li>Arguments separated by <code>...</code> can be repeated</li>
	 * </ul>
	 * <p>
	 */
	public String usage = "";

	public Option getOption(String name) {
		return Arrays.stream(args).filter(option -> option.name.equals(name)).findFirst().orElse(null);
	}

	public static CommandInfo from(String name, String description) {
		CommandInfo info = new CommandInfo();
		info.name = name;
		info.help = description;
		return info;
	}

	public static CommandInfo from(String name, String description, Option... arguments) {
		CommandInfo info = from(name, description);
		info.args = arguments;
		return info;
	}

	public static CommandInfo from(String name, String description, String usage, Option... arguments) {
		CommandInfo info = from(name, description, arguments);
		info.usage = usage;
		return info;
	}

	public static CommandInfo from(String name, String description, String usage, String[] aliases, Option... arguments) {
		CommandInfo info = from(name, description, usage, arguments);
		info.aliases = aliases;
		return info;
	}

	/**
	 * Creates a {@link CommandInfo} from an {@link Command} annotation
	 * Not recommended, as it does not include arguments
	 * @param annotation the instance of {@link Command} annotating the command
	 * @return the {@link CommandInfo} for the command
	 */
	@Deprecated(since = "1.0.0")
	public static CommandInfo from(Command annotation) {
		CommandInfo info = new CommandInfo();
		info.name = annotation.name();
		info.help = annotation.help();
		info.aliases = annotation.aliases();
		info.usage = annotation.usage();
		info.scope = annotation.scope();
		info.category = annotation.category();
		info.permission = annotation.permission();
		return info;
	}

	public static CommandInfo from(ICommand command) {
		Command annotation = command.getClass().getAnnotation(Command.class);
		if (annotation == null) {
			throw new IllegalArgumentException("Command must have @Command annotation");
		}
		CommandInfo info = from(annotation);
		info.args = command.getArguments();
		return info;
	}

	public static CommandInfo from(GenericCommandEvent event) {
		return from(getCommand(event.getCommandName()));
	}


	public record Option(String name, String description, OptionType type, boolean isRequired, boolean autocomplete, Function<GenericCommandEvent, Object> defaultValue, Predicate<String> validateArg) {
		public static Option required(String name, String description, OptionType type, boolean autocomplete) {
			return new Option(name, description, type, true, autocomplete, null, null);
		}

		public static Option optional(String name, String description, OptionType type, Function<GenericCommandEvent, Object> defaultValue, boolean autocomplete) {
			return new Option(name, description, type, false, autocomplete, defaultValue, null);
		}

		public static Option optional(String name, String description, OptionType type, Object defaultValue, boolean autocomplete) {
			return new Option(name, description, type, false, autocomplete, (ignored) -> defaultValue, null);
		}

		public static Option optional(String name, String description, OptionType type, boolean autocomplete) {
			return new Option(name, description, type, false, autocomplete, (ignored) -> null, null);
		}

		public static Option optional(String name, String description, OptionType type, Predicate<String> validateArg, boolean autocomplete) {
			return new Option(name, description, type, false, autocomplete, (ignored) -> null, validateArg);
		}

		public static Option optional(String name, String description, OptionType type, Function<GenericCommandEvent, Object> defaultValue, Predicate<String> validateArg, boolean autocomplete) {
			return new Option(name, description, type, false, autocomplete, defaultValue, validateArg);
		}

		public static Option optional(String name, String description, OptionType type, Object defaultValue, Predicate<String> validateArg, boolean autocomplete) {
			return new Option(name, description, type, false, autocomplete, (ignored) -> defaultValue, validateArg);
		}
	}



	public CommandInfo() {
		super();
	}

	@Override
	protected final Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public final boolean equals(Object o) {
		return this == o ||
				o instanceof CommandInfo &&
						name.equals(((CommandInfo) o).name) &&
						help.equals(((CommandInfo) o).help) &&
						usage.equals(((CommandInfo) o).usage) &&
						Arrays.equals(aliases, ((CommandInfo) o).aliases) &&
						Arrays.equals(args, ((CommandInfo) o).args);
	}

	@Override
	public final String toString() {
		return new Gson().toJson(this);
	}
}


