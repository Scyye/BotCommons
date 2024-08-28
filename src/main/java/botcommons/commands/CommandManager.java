package botcommons.commands;

import botcommons.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandManager extends ListenerAdapter {
	private static final Map<CommandInfo, Method> commands = new HashMap<>();
	private static final Map<String, List<Map.Entry<CommandInfo, Method>>> subcommands = new HashMap<>();
	private static Function<GenericCommandEvent, Boolean> commandRunCheck = $ -> true;

	private CommandManager() {}

	public static void init(JDA jda, Function<GenericCommandEvent, Boolean> commandRunCheck) {
		CommandManager.commandRunCheck = commandRunCheck;
		init(jda);
	}

	public static void init(JDA jda) {
		jda.addEventListener(new CommandManager());
	}

	public static void addCommands(Class<?>... holders) {
		for (Class<?> holder : holders) {
			CommandHolder meta = holder.getAnnotation(CommandHolder.class);
			if (meta == null) {
				throw new IllegalArgumentException("MethodCommandHolder annotation not found on class " + holder.getName());
			}
			if (meta.group().equalsIgnoreCase("n/a")) {
				registerCommands(holder);
			} else {
				addSubcommands(holder);
			}
		}
	}

	private static void registerCommands(Class<?> holder) {
		Arrays.stream(holder.getMethods())
				.filter(method -> method.isAnnotationPresent(Command.class))
				.map(CommandInfo::from)
				.forEach(info -> commands.put(info, info.method));
	}

	private static void addSubcommands(Class<?> holder) {
		CommandHolder meta = holder.getAnnotation(CommandHolder.class);
		if (meta == null || meta.group().equalsIgnoreCase("n/a")) {
			throw new IllegalArgumentException("Invalid CommandHolder group for class " + holder.getName());
		}

		String parent = meta.group();
		subcommands.putIfAbsent(parent, new ArrayList<>());

		Arrays.stream(holder.getMethods())
				.filter(method -> method.isAnnotationPresent(Command.class))
				.map(CommandInfo::from)
				.forEach(info -> subcommands.get(parent).add(new AbstractMap.SimpleEntry<>(info, info.method)));
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		List<SlashCommandData> commandData = createCommandData();

		event.getJDA().updateCommands().addCommands(commandData).queue(
				commands1 -> System.out.println(commands1 + " commands registered\n" +
						commands1.stream().map(command -> command.getSubcommands().size()).toList())
		);
	}

	private List<SlashCommandData> createCommandData() {
		List<SlashCommandData> commandData = commands.entrySet().stream()
				.map(entry -> createSlashCommand(entry.getKey()))
				.collect(Collectors.toList());

		subcommands.forEach((parent, subcommandList) -> {
			SlashCommandData slashData = Commands.slash(parent, parent);
			subcommandList.stream()
					.map(sub -> createSubcommand(sub.getKey()))
					.forEach(slashData::addSubcommands);
			commandData.add(slashData);
		});

		return commandData.stream()
				.filter(dad -> commandData.stream()
						.noneMatch(data1 -> data1.getName().equals(dad.getName()) || dad.getName().startsWith(data1.getName())))
				.collect(Collectors.toList());
	}

	private SlashCommandData createSlashCommand(CommandInfo info) {
		SlashCommandData data = Commands.slash(info.name, info.help);
		if (info.args != null) {
			Arrays.stream(info.args).forEach(option ->
					data.addOptions(createOptionData(option)));
		}
		return data;
	}

	private SubcommandData createSubcommand(CommandInfo info) {
		SubcommandData subData = new SubcommandData(info.name, info.help);
		if (info.args != null) {
			Arrays.stream(info.args).forEach(option ->
					subData.addOption(option.getType(), option.getName(), option.getDescription(),
							option.isRequired(), option.isAutocomplete()));
		}
		return subData;
	}

	private OptionData createOptionData(CommandInfo.Option option) {
		return new OptionData(option.getType(), option.getName(), option.getDescription(),
				option.isRequired(), option.isAutocomplete())
				.addChoices(option.getChoices().stream()
						.map(choice -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(choice, choice))
						.toList());
	}

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent slash) {
		GenericCommandEvent event = GenericCommandEvent.of(slash);
		CommandInfo info = CommandInfo.from(event);
		if (!commandRunCheck.apply(event)) {
			replyError(event, "There was an issue.");
			return;
		}

		Method cmd = getCommand(slash.getFullCommandName());
		if (cmd == null || !checks(info, event, cmd)) return;

		try {
			List<Object> args = prepareArguments(info, event);
			cmd.invoke(null, args.toArray());
		} catch (Exception e) {
			handleError(event, e);
		}
	}

	private List<Object> prepareArguments(CommandInfo info, GenericCommandEvent event) {
		List<Object> args = new ArrayList<>();
		args.add(event);
		Arrays.stream(info.args).forEach(option ->
				args.add(event.getArg(option.getName(), typeMap.get(option.getType()))));
		return args;
	}

	private void handleError(GenericCommandEvent event, Exception e) {
		e.printStackTrace();
		replyError(event, "An error occurred while executing this command");
		if (e.getMessage() != null) {
			replyError(event, e.getMessage().substring(0, Math.min(e.getMessage().length(), 2000)));
		}
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		CommandInfo info = CommandInfo.from(getCommand(event.getFullCommandName()));
		if (info == null) {
			event.replyChoiceStrings("Command not found").queue();
			return;
		}

		Method autocompleteMethod = findAutocompleteMethod(info);
		if (autocompleteMethod == null) {
			event.replyChoiceStrings("No autocomplete handler found for this command").queue();
			return;
		}

		invokeAutocompleteMethod(event, autocompleteMethod);
	}

	private Method findAutocompleteMethod(CommandInfo info) {
		return Arrays.stream(info.method.getDeclaringClass().getDeclaredMethods())
				.filter(method -> method.isAnnotationPresent(AutoCompleteHandler.class)
						&& method.getParameters().length == 1
						&& Arrays.asList(method.getAnnotation(AutoCompleteHandler.class).value())
						.contains(info.name))
				.findFirst()
				.orElse(null);
	}

	private void invokeAutocompleteMethod(CommandAutoCompleteInteractionEvent event, Method autocompleteMethod) {
		try {
			autocompleteMethod.invoke(autocompleteMethod.getDeclaringClass().getConstructors()[0].newInstance(), event);
		} catch (Exception e) {
			e.printStackTrace();
			event.replyChoiceStrings(e.getMessage().substring(0, Math.min(e.getMessage().length(), 15))).queue();
		}
	}

	private static boolean checks(CommandInfo info, GenericCommandEvent event, Method cmd) {
		if (!Config.getInstance().get("owner-id").equals(event.getUser().getId())
				&& !checkPermissions(info, event)) return false;

		if (!checkScope(info, event)) return false;

		return true;
	}

	private static boolean checkPermissions(CommandInfo info, GenericCommandEvent event) {
		if ("owner".equals(info.permission)) {
			event.replyError("You do not have permission to use this command").finish();
			return false;
		} else if (!event.getMember().hasPermission(Permission.valueOf(info.permission))) {
			event.replyError("You do not have permission to use this command").finish();
			return false;
		}
		return true;
	}

	private static boolean checkScope(CommandInfo info, GenericCommandEvent event) {
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
							event.replyError("This command can only be used in DMs\n" + privateChannel.getAsMention()).finish());
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
		return commands.entrySet().stream()
				.filter(entry -> entry.getKey().name.equalsIgnoreCase(command)
						|| Arrays.stream(entry.getKey().aliases).anyMatch(alias -> alias.equalsIgnoreCase(command)))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElseGet(() -> findSubcommand(command));
	}

	private static Method findSubcommand(String command) {
		for (var entry : subcommands.entrySet()) {
			if (command.startsWith(entry.getKey())) {
				return entry.getValue().stream()
						.filter(sub -> sub.getKey().name.equalsIgnoreCase(command.split(" ")[1])
								|| Arrays.stream(sub.getKey().aliases).anyMatch(alias -> alias.equalsIgnoreCase(command.split(" ")[1])))
						.map(Map.Entry::getValue)
						.findFirst()
						.orElse(null);
			}
		}
		return null;
	}

	private static void replyError(GenericCommandEvent event, String message) {
		if (!event.getSlashCommandInteraction().isAcknowledged()) {
			event.replyError(message).ephemeral().finish();
		}
	}
}
