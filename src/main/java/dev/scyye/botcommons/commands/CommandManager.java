package dev.scyye.botcommons.commands;

import dev.scyye.botcommons.config.GuildConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class CommandManager extends ListenerAdapter {
	public static List<ICommand> commands = new ArrayList<>();

	public CommandManager() {
	}

	public static void addCommands(ICommand... commands) {
		CommandManager.commands.addAll(Arrays.asList(commands));
	}

	@Override
	public void onReady(@NotNull ReadyEvent event) {
		List<SlashCommandData> data = new ArrayList<>();
		CommandManager.commands.forEach(command -> {
			CommandInfo info = CommandInfo.from(command);

			var d = Commands.slash(info.name, info.help);
			if (info.args != null) {
				Arrays.stream(info.args).forEachOrdered(option -> {
					d.addOption(option.type(), option.name(), option.description(),
							option.isRequired(), option.autocomplete());
				});
			}


			if (info.scope == Command.Scope.GUILD || info.scope == Command.Scope.TICKET)
				d.setGuildOnly(true);

			System.out.println(STR."Registered command \{info.name} (\{info.help}) with \{info.args != null ? info.args.length : 0} arguments");
			data.add(d);
		});
		event.getJDA().updateCommands().addCommands(data).queue();
		System.out.println("Registered " + data.size() + " commands");
	}

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
		ICommand cmd = getCommand(event.getName());

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
			case TICKET -> {
				if (!event.isFromGuild() || !event.getChannel().getName().startsWith("ticket-")) {
					event.reply("This command can only be used in a ticket!").queue();
					return;
				}
			}
		}

		GenericCommandEvent commandEvent = GenericCommandEvent.of(event);

		if (!event.getUser().getId().equals("553652308295155723")) {
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
			event.reply("An error occurred while executing the command!\nView log for more details").queue();
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
			case TICKET -> {
				if (!event.isFromGuild() || !event.getChannel().getName().startsWith("ticket-")) {
					event.getMessage().reply("This command can only be used in a ticket!").queue();
					return;
				}
			}
		}

		if (!event.getAuthor().getId().equals("553652308295155723")) {
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
		ICommand cmd = getCommand(event.getFullCommandName());

		if (cmd == null)
			return;


		try {
			cmd.handleAutocomplete(event);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ICommand getCommand(String command) {
		return commands.stream().filter(cmd -> Arrays.stream(CommandInfo.from(cmd).aliases).toList().contains(command)).findFirst().orElse(
				commands.stream().filter(cmd -> cmd.getCommandInfo().name.equals(command)).findFirst().orElse(null)
		);
	}
}
