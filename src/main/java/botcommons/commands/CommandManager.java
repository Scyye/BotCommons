package botcommons.commands;

import botcommons.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

public class CommandManager extends ListenerAdapter {
	private static final HashMap<CommandInfo, Method> commands = new HashMap<>();
	private static final HashMap<String, List<Map.Entry<CommandInfo, Method>>> subcommands = new HashMap<>();

	private CommandManager() {}

	private static Function<GenericCommandEvent, Boolean> commandRunCheck = ($) -> true;

	public static void init(JDA jda, Function<GenericCommandEvent, Boolean> commandRunCheck) {
		init(jda);
		CommandManager.commandRunCheck = commandRunCheck;
	}

	public static void init(JDA jda) {
		jda.addEventListener(new CommandManager());
	}

	public static void addCommands(Class<?>... holders) {
		for (var holder : holders) {
			CommandHolder meta = holder.getAnnotation(CommandHolder.class);
			if (meta == null) {
				throw new IllegalArgumentException("MethodCommandHolder annotation not found on class " + holder.getName());
			}
			if (meta.group().equalsIgnoreCase("n/a")) {
				for (var cmd : holder.getMethods()) {
					if (cmd.isAnnotationPresent(Command.class)) {
						CommandInfo info = CommandInfo.from(cmd);
						CommandManager.commands.put(info, cmd);
					}
				}
			} else {
				addSubcommands(holder);
				continue;
			}

			for (var cmd : holder.getMethods()) {
				if (cmd.isAnnotationPresent(Command.class)) {
					CommandInfo info = CommandInfo.from(cmd);
					CommandManager.commands.put(info, cmd);
				}
			}
		}
	}

	private static void addSubcommands(Class<?> holder) {
		CommandHolder meta = holder.getAnnotation(CommandHolder.class);
		if (meta == null) {
			throw new IllegalArgumentException("MethodCommandHolder annotation not found on class " + holder.getName());
		}
		if (meta.group().equalsIgnoreCase("n/a")) {
			throw new IllegalArgumentException("MethodCommandHolder annotation group is not set to a valid value");
		}
		String parent = meta.group();
		for (var cmd : holder.getMethods()) {
			if (cmd.isAnnotationPresent(Command.class)) {
				CommandInfo info = CommandInfo.from(cmd);
				subcommands.putIfAbsent(parent, new ArrayList<>());
				subcommands.get(parent).add(new AbstractMap.SimpleEntry<>(info, cmd));
			}
		}
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		List<SlashCommandData> commandData = new ArrayList<>();
		for (var entry : commands.entrySet()) {
			CommandInfo info = entry.getKey();
			SlashCommandData d = Commands.slash(info.name, info.help);
			if (info.args != null)
				Arrays.stream(info.args).forEachOrdered(option ->
						d.addOptions(new OptionData(
								option.getType(), option.getName(), option.getDescription(),
								option.isRequired(), option.isAutocomplete())
								.addChoices(option.getChoices().stream().map(choice ->
										new net.dv8tion.jda.api.interactions.commands.Command.Choice(choice, choice)).toList())));

			commandData.add(d);
		}

		for (var entry : subcommands.entrySet()) {
			SlashCommandData d = Commands.slash(entry.getKey(), entry.getKey());
			List<SubcommandData> subcommandData = new ArrayList<>();
			for (var sub : entry.getValue()) {
				CommandInfo info = sub.getKey();
				SubcommandData subData = new SubcommandData(info.name, info.help);
				if (info.args != null) {
					Arrays.stream(info.args).forEachOrdered(option ->
							subData.addOption(option.getType(), option.getName(), option.getDescription(),
									option.isRequired(), option.isAutocomplete()));
				}
				subcommandData.add(subData);
			}

			d.addSubcommands(subcommandData);
			commandData.add(d);
		}

		List<SlashCommandData> confirmedData = new ArrayList<>();
		for (var dad : commandData) {
			if (confirmedData.stream().noneMatch(data1 -> data1.getName().equals(dad.getName()) ||
					dad.getName().startsWith(data1.getName())))
				confirmedData.add(dad);
		}

		event.getJDA().updateCommands().addCommands(confirmedData).queue(commands1 ->
				System.out.println(commands1 + " commands registered\n"+commands1.stream().map(
						command -> command.getSubcommands().size()).toList()
				));
	}

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent slash) {
		GenericCommandEvent event = GenericCommandEvent.of(slash);
		CommandInfo info = CommandInfo.from(event);

		if (!commandRunCheck.apply(event)) {
			if (!event.getSlashCommandInteraction().isAcknowledged()) {
				event.replyError("There was an issue.").ephemeral().finish();
			}
			return;
		}

		Method cmd = getCommand(slash.getFullCommandName());

		if (!checks(info, event, cmd)) return;

		try {
			List<Object> args = new ArrayList<>();
			args.add(event);
			for (var option : info.args) {
				args.add(event.getArg(option.getName(), typeMap.get(option.getType())));
			}
			if (cmd != null)
				cmd.invoke(null, args.toArray());
			else
				event.replyError("Could not find the requested command.").finish();
		} catch (Exception e) {
			e.printStackTrace();
			event.replyError("An error occurred while executing this command");
			if (e.getMessage() != null)
				event.replyError(e.getMessage().substring(0, Math.min(e.getMessage().length(), 2000))).finish();
		}
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		CommandInfo info = CommandInfo.from(getCommand(event.getFullCommandName()));
		if (info == null) {
			event.replyChoiceStrings("Command not found").queue();
			return;
		}
		Class<?> clazz = info.method.getDeclaringClass();

		Method autocompleteMethod = findAutoCompleteMethod(clazz, event.getFullCommandName());

		if (autocompleteMethod == null) {
			event.replyChoiceStrings("No autocomplete handler found for this command").queue();
			return;
		}

		try {
			// run the method, with it being static.
			autocompleteMethod.invoke(clazz.getConstructors()[0].newInstance(), event);
		} catch (Exception e) {
			e.printStackTrace();
			event.replyChoiceStrings(e.getMessage().substring(0, Math.min(e.getMessage().length(), 15))).queue();
		}
	}

	private static boolean checks(CommandInfo info, GenericCommandEvent event, Method cmd) {
		if (cmd == null || event.getMember() == null) {
			event.replyError("Command not found").finish();
			return false;
		}
		if (Config.getInstance().get("owner-id").equals(event.getUser().getId())) {
			return true;
		}
		if (Objects.equals(info.permission, "owner")) {
			if (!Config.getInstance().get("owner-id").equals(event.getUser().getId())) {
				event.replyError("You do not have permission to use this command").finish();
				return false;
			}
			return true;
		} else
		if (!event.getMember().hasPermission(Permission.valueOf(info.permission))) {
			event.replyError("You do not have permission to use this command").finish();
			return false;
		}

		switch (info.scope) {
			case GUILD -> {
				if (!event.isGuild()) {
					event.replyError("This command can only be used in a guild").finish();
					return false;
				}
			}
			case DM -> {
				if (event.isGuild()) {
					event.getUser().openPrivateChannel().queue(privateChannel ->
							event.replyError("This command can only be used in DMs\n"+privateChannel.getAsMention()).finish());
					return false;
				}
			}
		}
		return true;
	}

	private static final HashMap<OptionType, Class<?>> typeMap = new HashMap<>(){{
		put(OptionType.STRING, String.class);
		put(OptionType.INTEGER, Integer.class);
		put(OptionType.BOOLEAN, Boolean.class);
		put(OptionType.USER, User.class);
		put(OptionType.CHANNEL, Channel.class);
		put(OptionType.ATTACHMENT, Message.Attachment.class);
		put(OptionType.ROLE, Role.class);
		put(OptionType.MENTIONABLE, Object.class);
		put(OptionType.NUMBER, Double.class);
		put(OptionType.SUB_COMMAND, String.class);
		put(OptionType.SUB_COMMAND_GROUP, String.class);
		put(OptionType.UNKNOWN, Object.class);
	}};

	public static Method getCommand(String command) {
		// First, check if the command is a direct match or an alias
		Method possibleCommand = commands.entrySet().stream()
				.filter(entry -> entry.getKey().name.equalsIgnoreCase(command)
						|| Arrays.stream(entry.getKey().aliases).anyMatch(alias -> alias.equalsIgnoreCase(command)))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse(null);

		// If a direct match is found, return it
		if (possibleCommand != null) {
			return possibleCommand;
		}

		// If not a direct command, check subcommands
		for (var entry : subcommands.entrySet()) {
			String parentCommand = entry.getKey();
			List<Map.Entry<CommandInfo, Method>> subcommandList = entry.getValue();

			// Check if the command starts with the parent command
			if (!command.startsWith(parentCommand)) continue;

			// If the parent command matches exactly, check for matching subcommands
			if (parentCommand.equalsIgnoreCase(command)) {
				return subcommandList.stream()
						.filter(subcommand -> subcommand.getKey().name.equalsIgnoreCase(command)
								|| Arrays.stream(subcommand.getKey().aliases).anyMatch(alias -> alias.equalsIgnoreCase(command)))
						.map(Map.Entry::getValue)
						.findFirst()
						.orElse(null);
			}

			// Split the command to separate the parent command and the subcommand
			String[] commandParts = command.split(" ");
			if (commandParts.length < 2) continue;  // If no subcommand, skip

			String group = commandParts[0];
			String subcommand = commandParts[1];

			// If the group (parent command) matches, check for matching subcommands
			if (parentCommand.equalsIgnoreCase(group)) {
				Method matchedSubcommand = subcommandList.stream()
						.filter(sub -> sub.getKey().name.equalsIgnoreCase(subcommand)
								|| Arrays.stream(sub.getKey().aliases).anyMatch(alias -> alias.equalsIgnoreCase(subcommand)))
						.map(Map.Entry::getValue)
						.findFirst()
						.orElse(null);

				if (matchedSubcommand != null) {
					return matchedSubcommand;
				}
			}
		}

		// If no command is found, return null
		return null;
	}

	private static Method findAutoCompleteMethod(Class<?> clazz, String commandName) {
		return Arrays.stream(clazz.getDeclaredMethods())
				.filter(method -> method.isAnnotationPresent(AutoCompleteHandler.class)
						&& method.getParameters().length == 1
						&& Arrays.stream(method.getAnnotation(AutoCompleteHandler.class).value()).toList().contains(commandName))
				.findFirst().orElse(null);
	}
}
