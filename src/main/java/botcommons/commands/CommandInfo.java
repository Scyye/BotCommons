package botcommons.commands;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

/**
 * This class holds information about a command.
 */
@SuppressWarnings("unused")
public class CommandInfo {
	private static final HashMap<Class<?>, OptionType> optionTypeParams = new HashMap<>() {{
		put(String.class, OptionType.STRING);
		put(Integer.class, OptionType.INTEGER);
		put(int.class, OptionType.INTEGER);
		put(Boolean.class, OptionType.BOOLEAN);
		put(boolean.class, OptionType.BOOLEAN);
		put(Long.class, OptionType.INTEGER);
		put(long.class, OptionType.INTEGER);
		put(Double.class, OptionType.NUMBER);
		put(double.class, OptionType.NUMBER);
		put(Float.class, OptionType.NUMBER);
		put(float.class, OptionType.NUMBER);
		put(Short.class, OptionType.INTEGER);
		put(short.class, OptionType.INTEGER);
		put(Byte.class, OptionType.INTEGER);
		put(byte.class, OptionType.INTEGER);
		put(Character.class, OptionType.STRING);
		put(List.class, OptionType.STRING);
		put(ArrayList.class, OptionType.STRING);
		put(Object.class, OptionType.STRING);
		put(Object[].class, OptionType.STRING);

		put(User.class, OptionType.USER);
		put(Role.class, OptionType.ROLE);
		put(Message.Attachment.class, OptionType.ATTACHMENT);
		put(TextChannel.class, OptionType.CHANNEL);
	}};
	/**
	 * The command name. Should be unique, and lowercase
	 */
	public String name;
	public String help;
	public String[] aliases;
	public CommandInfo.Option[] args;
	public InteractionContextType[] userContext;
	public String category;
	public String permission;
	public String usage = "";
	public Method method;

	/**
	 * Get the option with the specified name.
	 * @param name the name of the option to retrieve
	 * @return the {@link CommandInfo.Option} with the specified name, or null if no such option exists
	 */
	public CommandInfo.Option getOption(String name) {
		return Arrays.stream(args).filter(option -> option.name.equals(name)).findFirst().orElse(null);
	}

	/**
	 * Creates a CommandInfo instance from a method with the {@link Command} annotation.
	 * @param command the method to create the {@link CommandInfo} from. This method must have the {@link Command} annotation.
	 * @return a {@link CommandInfo} instance containing the information from the method.
	 */
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
		info.userContext = annotation.userContext();
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
					.type(optionTypeParams.getOrDefault(param.getType(), paramAnnotation.type()))
					.required(paramAnnotation.required())
					.autocomplete(paramAnnotation.autocomplete())
					.name(param.getName())
					.choices(paramAnnotation.choices());

			args.add(option);
		}
		info.args = args.toArray(new CommandInfo.Option[0]);
		return info;
	}

	/**
	 * Creates a CommandInfo instance from a {@link GenericCommandEvent}.
	 * @param event the {@link GenericCommandEvent} to create the {@link CommandInfo} from. This event should be associated with a command.
	 * @return a {@link CommandInfo} instance containing the information from the event.
	 */
	public static CommandInfo from(GenericCommandEvent event) {
		boolean sub = event.getSubcommandName() != null || event.getSubcommandGroup() != null;
		String command = sub ? event.getCommandName() + (event.getSubcommandGroup()!=null? " " + event.getSubcommandGroup() + " ": " ") +
				event.getSubcommandName() : event.getCommandName();

		return from(CommandManager.getCommand(command));
	}

	/**
	 * Represents an option (argument) for a command.
	 */
	@Getter
	public static class Option {
		private String name;
		private String description;
		private OptionType type;
		private boolean isRequired;
		private boolean autocomplete;
		private String defaultValue;
		private Predicate<String> validateArg;
		private List<String> choices;

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

		public Option choices(String... choices) {
			this.choices = Arrays.asList(choices);
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
		return "{" +
				"\"name\":\"" + name + "\"," +
				"\"help\":\"" + help + "\"," +
				"\"aliases\":" + Arrays.toString(aliases) + "," +
				"\"args\":" + Arrays.toString(args) + "," +
				"\"category\":\"" + category + "\"," +
				"\"permission\":\"" + permission + "\"," +
				"\"usage\":\"" + usage + "\"," +
				"\"method\":\"" + method + "\"" +
				"}";
	}
}
