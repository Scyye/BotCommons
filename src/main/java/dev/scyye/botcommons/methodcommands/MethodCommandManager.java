package dev.scyye.botcommons.methodcommands;

import dev.scyye.botcommons.commands.GenericCommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.*;

public class MethodCommandManager extends ListenerAdapter {
	private static final HashMap<MethodCommandInfo, Method> commands = new HashMap<>();
	private static final HashMap<String, List<Map.Entry<MethodCommandInfo, Method>>> subcommands = new HashMap<>();

	public static void addCommands(Object... holders) {
		for (var holder : holders) {
			MethodCommandHolder meta = holder.getClass().getAnnotation(MethodCommandHolder.class);
			if (meta == null) {
				throw new IllegalArgumentException("MethodCommandHolder annotation not found on class " + holder.getClass().getName());
			}
			if (meta.group().equalsIgnoreCase("n/a")) {
				for (var cmd : holder.getClass().getMethods()) {
					if (cmd.isAnnotationPresent(MethodCommand.class)) {
						MethodCommandInfo info = MethodCommandInfo.from(cmd);
						MethodCommandManager.commands.put(info, cmd);
					}
				}
			} else {
				addSubcommands(holder);
				continue;
			}

			for (var cmd : holder.getClass().getMethods()) {
				if (cmd.isAnnotationPresent(MethodCommand.class)) {
					MethodCommandInfo info = MethodCommandInfo.from(cmd);
					MethodCommandManager.commands.put(info, cmd);
				}
			}
		}
	}

	public static void addSubcommands(Object holder) {
		MethodCommandHolder meta = holder.getClass().getAnnotation(MethodCommandHolder.class);
		if (meta == null) {
			throw new IllegalArgumentException("MethodCommandHolder annotation not found on class " + holder.getClass().getName());
		}
		if (meta.group().equalsIgnoreCase("n/a")) {
			throw new IllegalArgumentException("MethodCommandHolder annotation group is not set to a valid value");
		}
		String parent = meta.group();
		for (var cmd : holder.getClass().getMethods()) {
			if (cmd.isAnnotationPresent(MethodCommand.class)) {
				MethodCommandInfo info = MethodCommandInfo.from(cmd);
				subcommands.putIfAbsent(parent, new ArrayList<>());
				subcommands.get(parent).add(new AbstractMap.SimpleEntry<>(info, cmd));
			}
		}
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		List<SlashCommandData> data = new ArrayList<>();
		for (var entry : commands.entrySet()) {
			MethodCommandInfo info = entry.getKey();
			SlashCommandData d = Commands.slash(info.name, info.help);
			if (info.args != null) {
				Arrays.stream(info.args).forEachOrdered(option ->
						d.addOption(option.getType(), option.getName(), option.getDescription(),
								option.isRequired(), option.isAutocomplete()));
			}
			data.add(d);
		}

		for (var entry : subcommands.entrySet()) {
			SlashCommandData d = Commands.slash(entry.getKey(), entry.getKey());
			List<SubcommandData> commandData = new ArrayList<>();
			for (var sub : entry.getValue()) {
				MethodCommandInfo info = sub.getKey();
				SubcommandData subData = new SubcommandData(info.name, info.help);
				if (info.args != null) {
					Arrays.stream(info.args).forEachOrdered(option ->
							subData.addOption(option.getType(), option.getName(), option.getDescription(),
									option.isRequired(), option.isAutocomplete()));
				}
				commandData.add(subData);
			}

			d.addSubcommands(commandData);
			data.add(d);
		}

		List<SlashCommandData> confirmedData = new ArrayList<>();
		for (var dad : data) {
			if (confirmedData.stream().noneMatch(data1 -> data1.getName().equals(dad.getName()) ||
					dad.getName().startsWith(data1.getName())))
				confirmedData.add(dad);
		}

		event.getJDA().updateCommands().addCommands(confirmedData).queue(commands1 ->
				System.out.println(commands1 + " commands registered"));
	}

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent slash) {
		GenericCommandEvent event = GenericCommandEvent.of(slash);
		MethodCommandInfo info = MethodCommandInfo.from(event);
		//System.out.println(slash.getFullCommandName());
		Method cmd = getCommand(slash.getFullCommandName());

		if (cmd == null) {
			event.replyError("Command not found");
			return;
		}
		if (info.permission != null && !event.getMember().hasPermission(Permission.valueOf(info.permission))) {
			event.replyError("You do not have permission to use this command");
			return;
		}
		switch (info.scope) {
			case GUILD -> {
				if (!event.isGuild()) {
					event.replyError("This command can only be used in a guild");
					return;
				}
			}
			case DM -> {
				if (event.isGuild()) {
					event.getUser().openPrivateChannel().queue(privateChannel ->
							event.replyError("This command can only be used in DMs\n"+privateChannel.getAsMention()));
					return;
				}
			}
		}
		try {
			List<Object> args = new ArrayList<>();
			args.add(event);
			for (var option : info.args) {
				args.add(event.getArg(option.getName(), typeMap.get(option.getType())));
			}
			cmd.invoke(null, args.toArray());
		} catch (Exception e) {
			e.printStackTrace();
			event.replyError("An error occurred while executing this command");
			if (e.getMessage() != null)
				event.replyError(e.getMessage().substring(0, Math.min(e.getMessage().length(), 2000)));
		}
	}

	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		MethodCommandInfo info = MethodCommandInfo.from(getCommand(event.getFullCommandName()));
		Class<?> clazz = info.method.getDeclaringClass();
		MethodCommandHolder meta = clazz.getAnnotation(MethodCommandHolder.class);
		Method autocomplete = Arrays.stream(clazz.getMethods()).filter(
				method -> method.isAnnotationPresent(AutoCompleteHandler.class) && method.getParameters().length == 1
				&& Arrays.stream(method.getAnnotation(AutoCompleteHandler.class).value()).toList().stream().map(s ->
						meta.group().equalsIgnoreCase("n/a") ? s : meta.group() + " " + s
				).toList().contains(event.getFullCommandName())
		).findFirst().orElse(null);
		if (autocomplete == null) {
			event.replyChoiceStrings("No autocomplete handler found for this command").queue();
			return;
		}

		try {
			autocomplete.invoke(null, event);
		} catch (Exception e) {
			e.printStackTrace();
			event.replyChoiceStrings(e.getMessage().substring(0, Math.min(e.getMessage().length(), 15))).queue();
		}
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent e) {
		GenericCommandEvent event = GenericCommandEvent.of(e);
		if (event.getUser().isBot()) return;
		if (event.getMessage().getContentRaw().startsWith(event.getPrefix())) {
			String command = event.getMessage().getContentRaw().substring(event.getPrefix().length());
			Method cmd = getCommand(command);
			if (cmd == null) {
				return;
			}
			try {
				List<Object> args = new ArrayList<>();
				args.add(event);
				for (var option : MethodCommandInfo.from(cmd).args) {
					//System.out.println(isSubcommandArgument(MethodCommandInfo.from(cmd), event.getCommandName()));
					if (isSubcommandArgument(event.getArg(option.getName(), String.class), event.getCommandName())) {
						continue;
					}
					//System.out.println(event.getArg(option.getName(), typeMap.get(option.getType())));
					args.add(event.getArg(option.getName(), typeMap.get(option.getType())));
				}
				// the rest of the params should be null
				for (int i = args.size(); i < cmd.getParameterCount(); i++) {
					args.add(null);
				}

				cmd.invoke(null, args.toArray());
			} catch (Exception ex) {
				ex.printStackTrace();
				event.getChannel().sendMessage("An error occurred while executing this command").queue();
			}
		}
	}

	private static boolean isSubcommandArgument(String arg, String command) {
		System.out.println("arg: " + arg);
		System.out.println("command: " + command);
		return subcommands.containsKey(command) && subcommands.get(command).stream().anyMatch(
				entry -> entry.getKey().name.equalsIgnoreCase(arg) ||
						Arrays.stream(entry.getKey().aliases).anyMatch(alias -> alias.equalsIgnoreCase(arg))
		);
	}

	private static final HashMap<OptionType, Class<?>> typeMap = new HashMap<>(){{
		put(OptionType.STRING, String.class);
		put(OptionType.INTEGER, Integer.class);
		put(OptionType.BOOLEAN, Boolean.class);
		put(OptionType.USER, User.class);
		put(OptionType.CHANNEL, Channel.class);
		put(OptionType.ROLE, Role.class);
		put(OptionType.MENTIONABLE, Object.class);
		put(OptionType.NUMBER, Double.class);
		put(OptionType.SUB_COMMAND, String.class);
		put(OptionType.SUB_COMMAND_GROUP, String.class);
		put(OptionType.UNKNOWN, Object.class);
	}};

	public static Method getCommand(String command) {
		Method possible = commands.entrySet().stream().filter(
				entry -> entry.getKey().name.equalsIgnoreCase(command) || Arrays.stream(entry.getKey().aliases).anyMatch(alias -> alias.equalsIgnoreCase(command))
		).map(Map.Entry::getValue).findFirst().orElse(null);

		if (possible != null) {
			return possible;
		}

		for (var entry : subcommands.entrySet()) {
			var name = entry.getKey();
			var subcommands = entry.getValue();
			if (name.equalsIgnoreCase(command)) {
				return subcommands.stream().filter(
						subcommand -> subcommand.getKey().name.equalsIgnoreCase(command) || Arrays.stream(subcommand.getKey().aliases)
								.anyMatch(alias -> alias.equalsIgnoreCase(command))
				).map(Map.Entry::getValue).findFirst().orElse(null);
			}
			String group = command.split(" ")[0];
			if (name.equalsIgnoreCase(group)) {
				Method p = subcommands.stream().filter(
						subcommand -> subcommand.getKey().name.equalsIgnoreCase(command.split(" ")[1]) || Arrays.stream(subcommand.getKey().aliases)
								.anyMatch(alias -> alias.equalsIgnoreCase(command.split(" ")[1]))
				).map(Map.Entry::getValue).findFirst().orElse(null);
				if (p != null) {
					return p;
				}
			}
			String subcommand = command.split(" ")[1];
			Method sub = entry.getValue().stream().filter(
					s -> s.getKey().name.equalsIgnoreCase(subcommand) || Arrays.stream(s.getKey().aliases)
							.anyMatch(alias -> alias.equalsIgnoreCase(s.getValue().getName()))
			).map(Map.Entry::getValue).findFirst().orElse(null);

			if (sub != null) {
				return sub;
			}
		}
		return null;
	}
}
