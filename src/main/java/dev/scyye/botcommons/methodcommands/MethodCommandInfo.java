package dev.scyye.botcommons.methodcommands;

import com.google.gson.Gson;
import dev.scyye.botcommons.commands.Command;
import dev.scyye.botcommons.methodcommands.MethodCommandInfo;
import dev.scyye.botcommons.commands.GenericCommandEvent;
import dev.scyye.botcommons.commands.ICommand;
import lombok.Getter;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import static dev.scyye.botcommons.methodcommands.MethodCommandManager.getCommand;

public class MethodCommandInfo {
	/**
	 * The command name. Should be unique, and lowercase
	 */
	public String name;
	public String help;
	public String[] aliases;
	public MethodCommandInfo.Option[] args;
	public MethodCommand.Scope scope;
	public String category;
	public String permission;
	public String usage = "";
	public Method method;

	public MethodCommandInfo.Option getOption(String name) {
		return Arrays.stream(args).filter(option -> option.name.equals(name)).findFirst().orElse(null);
	}

	public static MethodCommandInfo from(Method command) {
		MethodCommand annotation = command.getAnnotation(MethodCommand.class);
		if (annotation == null) {
			throw new IllegalArgumentException("Command must have @Command annotation");
		}
		MethodCommandInfo info = new MethodCommandInfo();
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
					.name(paramAnnotation.name());

			args.add(option);
		}
		info.args = args.toArray(new MethodCommandInfo.Option[0]);
		return info;
	}

	public static MethodCommandInfo from(GenericCommandEvent event) {
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
				o instanceof MethodCommandInfo &&
						name.equals(((MethodCommandInfo) o).name) &&
						help.equals(((MethodCommandInfo) o).help) &&
						usage.equals(((MethodCommandInfo) o).usage) &&
						Arrays.equals(aliases, ((MethodCommandInfo) o).aliases) &&
						Arrays.equals(args, ((MethodCommandInfo) o).args);
	}

	@Override
	public final String toString() {
		return new Gson().toJson(this);
	}
}
