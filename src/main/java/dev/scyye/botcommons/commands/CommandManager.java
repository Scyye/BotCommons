package dev.scyye.botcommons.commands;

import com.google.gson.Gson;
import dev.scyye.botcommons.config.GuildConfig;
import dev.scyye.botcommons.menu.Menu;
import dev.scyye.botcommons.menu.MenuManager;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Use the new {@link dev.scyye.botcommons.methodcommands.MethodCommandManager} and other method command classes instead
 */
@Deprecated
@ForRemoval
public class CommandManager extends ListenerAdapter {
	public static List<ICommand> commands = new ArrayList<>();
	public static HashMap<String, List<ICommand>> subcommands = new HashMap<>();

	public CommandManager() {
	}

	public static void addCommands(ICommand... commands) {
		CommandManager.commands.addAll(Arrays.asList(commands));
	}

	public static void addSubcommands(String parent, ICommand... subcommands) {
		CommandManager.subcommands.putIfAbsent(parent, Arrays.asList(subcommands));
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		List<SlashCommandData> data = new ArrayList<>();

		CommandManager.commands.forEach(command -> {
			if (!subcommands.containsKey(command.getCommandInfo().name)) {
				CommandInfo info = command.getCommandInfo();

				SlashCommandData d = Commands.slash(info.name, info.help);

				if (info.args != null) {
					Arrays.stream(info.args).forEachOrdered(option -> {
						d.addOption(option.type(), option.name(), option.description(), option.isRequired(), option.autocomplete());
					});
				}

				data.add(d);
			}
		});
		for (Map.Entry<String, List<ICommand>> entry : subcommands.entrySet()) {
			SlashCommandData d = Commands.slash(entry.getKey(), entry.getKey());

			List<SubcommandData> commandData = new ArrayList<>();
			entry.getValue().forEach(subcommand -> {
				CommandInfo info = subcommand.getCommandInfo();
				SubcommandData sub = new SubcommandData(info.name, info.help);


				if (info.args != null) {
					Arrays.stream(info.args).forEachOrdered(option -> {
						sub.addOption(option.type(), option.name(), option.description(), option.isRequired(), option.autocomplete());
					});
				}

				commandData.add(sub);
			});
			d.addSubcommands(commandData);

			data.add(d);
		}
		event.getJDA().updateCommands().addCommands(data).queue();
		System.out.println("Registered " + data.size() + " commands");
	}

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
		boolean sub = event.getSubcommandName() != null || event.getSubcommandGroup() != null;
		String command = sub ? event.getName() + (event.getSubcommandGroup()!=null? " " + event.getSubcommandGroup() + " ": " ") +
				event.getSubcommandName() : event.getName();

		ICommand cmd = getCommand(command);

		if (cmd == null) {
			event.reply("Command not found!").queue();
			return;
		}

		switch (cmd.getCommandInfo().scope) {
			case GUILD -> {
				if (!event.isFromGuild()) {
					event.reply("This command can only be used in a server!").queue();
					return;
				}
			}
			case DM -> {
				if (event.isFromGuild()) {
					event.reply("This command can only be used in a private message!").queue();
					return;
				}
			}
		}

		GenericCommandEvent commandEvent = GenericCommandEvent.of(event);

		if (!event.getUser().getId().equals("553652308295155723")) {
			if (cmd.getCommandInfo().permission.equals("admin")) {
				commandEvent.replyEphemeral("You do not have permission to use this command!");
				return;
			}
			if (Arrays.stream(Permission.values()).map(p -> p.getName().toLowerCase()).toList().contains(cmd.getCommandInfo().permission.toLowerCase())) {
				if (!event.getMember().hasPermission(Permission.valueOf(cmd.getCommandInfo().permission)) && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					commandEvent.replyEphemeral("You do not have permission to use this command!");
					return;
				}
			}
		}

		try {
			cmd.handle(commandEvent);
		} catch (Exception e) {
			if (event.isAcknowledged()) {
				event.getHook().sendMessage("An error occurred while executing the command!\nView log for more details").queue();
			} else {
				event.reply("An error occurred while executing the command!\nView log for more details").queue();
			}
			e.printStackTrace();
		}
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return;

		String prefix = GuildConfig.fromGuildId(event.getGuild().getId()).get("prefix");

		String content = event.getMessage().getContentRaw();
		if (!content.startsWith(prefix) && !event.getMessage().getContentRaw().startsWith(event.getJDA().getSelfUser().getAsMention()))
			return;


		String command = content.replaceFirst(STR."\{event.getJDA().getSelfUser().getAsMention()} ", "").split(" ")[0];
		if (command.startsWith(prefix)) {
			command = command.substring(prefix.length());
		}

		command = command.startsWith(" ") ? command.substring(1) : command;

		ICommand cmd = getCommand(command);

		GenericCommandEvent commandEvent = GenericCommandEvent.of(event);

		if (cmd == null) {
			event.getMessage().reply("Command not found!").queue();
			return;
		}

		switch (cmd.getCommandInfo().scope) {
			case GUILD -> {
				if (!event.isFromGuild()) {
					event.getMessage().reply("This command can only be used in a server!").queue();
					return;
				}
			}
			case DM -> {
				if (event.isFromGuild()) {
					event.getMessage().reply("This command can only be used in a private message!").queue();
					return;
				}
			}
		}

		if (!event.getAuthor().getId().equals("553652308295155723")) {
			if (cmd.getCommandInfo().permission.equals("admin")) {
				commandEvent.replyEphemeral("You do not have permission to use this command!");
				return;
			}
			if (Arrays.stream(Permission.values()).map(p -> p.name().toLowerCase()).toList().contains(cmd.getCommandInfo().permission.toLowerCase())) {
				if (!event.getMember().hasPermission(Permission.valueOf(cmd.getCommandInfo().permission)) && !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
					commandEvent.replyEphemeral("You do not have permission to use this command!");
					return;
				}
			}
		}

		try {
			cmd.handle(commandEvent);
		} catch (IllegalArgumentException e) {
			if (Objects.equals(cmd.getCommandInfo().usage, "")) {
				cmd.getCommandInfo().usage = Arrays.stream(cmd.getCommandInfo().args).map(CommandInfo.Option::name).toList().toString();
			}
			String errorMessage = e.getMessage().equals("Missing closing quote")
					? "Missing closing quotes\nIf you want to use a quote in an argument, simply use 2 single quotes `''`"
					: STR."Invalid arguments, usage: \n`\{cmd.getCommandInfo().usage}`";

			event.getMessage().reply(errorMessage).queue();
			e.printStackTrace();
		} catch (Exception e) {
			event.getMessage().reply("An error occurred while executing the command!\nView log for more details").queue();
			e.printStackTrace();
		}
	}

	@Override
	public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
		boolean sub = event.getSubcommandName() != null || event.getSubcommandGroup() != null;
		String command = sub ? event.getName() + (event.getSubcommandGroup()!=null? " " + event.getSubcommandGroup() + " ": " ") +
				event.getSubcommandName() : event.getName();

		ICommand cmd = getCommand(command);

		if (cmd == null)
			return;


		try {
			cmd.handleAutocomplete(event);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ICommand getCommand(String command) {
		ICommand tempReturn = null;
		for (ICommand cmd : CommandManager.commands) {
			if (command.contains(" ") && subcommands.get(cmd) != null) {
				for (ICommand sub : subcommands.get(cmd)) {
					if (sub.getCommandInfo().name.equalsIgnoreCase(command.split(" ")[1])) {
						return sub;
					}
				}

			}
			if (cmd.getCommandInfo().name.equalsIgnoreCase(command)) {
				return cmd;
			}
			if (command.startsWith(cmd.getCommandInfo().name)) {
				if (command.length()>cmd.getCommandInfo().name.length()) {
					// Theres a subcommand
					for (ICommand sub : subcommands.get(cmd)) {
						if (sub.getCommandInfo().name.equalsIgnoreCase(command.substring(cmd.getCommandInfo().name.length()))) {
							tempReturn = sub;
						}
					}
				} else
					tempReturn = cmd;
			}
		}
		System.out.println("Command not found");
		return tempReturn;
	}
}
