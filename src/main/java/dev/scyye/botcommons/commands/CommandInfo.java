package dev.scyye.botcommons.commands;

import com.google.gson.Gson;
import lombok.Getter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static dev.scyye.botcommons.commands.CommandManager.getCommand;

@SuppressWarnings("unused")
public class CommandInfo {
	/**
	 * The command name. Should be unique, and lowercase
	 */
	public String name;
	public String help;
	public String[] aliases;
	public CommandInfo.Option[] args;
	public Command.Scope scope;
	public String category;
	public String permission;
	public String usage = "";
	public Method method;

	public CommandInfo.Option getOption(String name) {
		return Arrays.stream(args).filter(option -> option.name.equals(name)).findFirst().orElse(null);
	}

	public static CommandInfo from(@Nullable Method command) {
		if (command == null)
			return null;

		Command annotation = command.getAnnotation(Command.class);
		if (annotation == null) {
			throw new IllegalArgumentException("Command must have @Command annotation");
		}
		CommandInfo info = new CommandInfo();
		info.name = annotation.name();
		info.help = annotation.help();
		info.aliases = annotation.aliases();
		info.usage = annotation.usage();
		info.scope = annotation.scope();
		info.category = annotation.category();
		info.permission = annotation.permission();
		info.method = command;

		List<Option> args = new ArrayList<>();
		for (var param : command.getParameters()) {
			if (param.getType() == GenericCommandEvent.class)
				continue;

			Param paramAnnotation = param.getAnnotation(Param.class);
			if (paramAnnotation == null) {
				throw new IllegalArgumentException("Parameter must have @Param annotation");
			}
			Option option = new Option()
					.description(paramAnnotation.description())
					.type(paramAnnotation.type())
					.required(paramAnnotation.required())
					.autocomplete(paramAnnotation.autocomplete())
					.name(param.getName());

			args.add(option);
		}
		info.args = args.toArray(new CommandInfo.Option[0]);
		return info;
	}

	public static CommandInfo from(GenericCommandEvent event) {
		boolean sub = event.getSubcommandName() != null || event.getSubcommandGroup() != null;
		String command = sub ? event.getCommandName() + (event.getSubcommandGroup()!=null? " " + event.getSubcommandGroup() + " ": " ") +
				event.getSubcommandName() : event.getCommandName();

		return from(getCommand(command));
	}

	@Getter
	public static class Option {
		private String name;
		private String description;
		private OptionType type;
		private boolean isRequired;
		private boolean autocomplete;
		private String defaultValue;
		private Predicate<String> validateArg;

		public Option name(String name) {
			this.name = name;
			return this;
		}

		public Option description(String description) {
			this.description = description;
			return this;
		}

		public Option type(OptionType type) {
			this.type = type;
			return this;
		}

		public Option required(boolean required) {
			isRequired = required;
			return this;
		}

		public Option autocomplete(boolean autocomplete) {
			this.autocomplete = autocomplete;
			return this;
		}

		public Option defaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}

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
