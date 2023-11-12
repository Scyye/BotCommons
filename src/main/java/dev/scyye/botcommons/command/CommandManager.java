package dev.scyye.botcommons.command;

import dev.scyye.botcommons.command.impl.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandManager extends ListenerAdapter {
	public static List<Command> commands = new ArrayList<>();
	public static Map<GroupCommand, List<Command>> subCommands = new HashMap<>();
	public static String prefix = "!";

	public static void addCommand(TextCommand command) {
		commands.add(command);
	}

	public static void addSubCommand(GroupCommand group, Command... commands) {
		subCommands.putIfAbsent(group, Arrays.stream(commands).toList());
	}

	public static void init(boolean addHelp, boolean addTree) {
		if (addHelp) {
			addCommand(new HelpCommand());
		}
		if (addTree) {
			addCommand(new TreeCommand());
		}
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		String[] splitMessage = event.getMessage().getContentRaw().split(" ");

		if (!splitMessage[0].startsWith("!")) return;

		String name = splitMessage[0].replaceFirst("!", "");
		Command command = isValid(name);

		if (command instanceof TextCommand textCommand) {
			System.out.println("Command found: " + textCommand.getName());
			textCommand.execute(CommandEvent.from(event), Arrays.copyOfRange(splitMessage, 1, splitMessage.length));
			return;
		}

		if (!(command instanceof GroupCommand groupCommand)) {
			reply(event, "Command not found. [`" + name + "`]");
			return;
		}

		System.out.println("Group command found: " + groupCommand.getName());

		if (splitMessage.length == 1) {
			reply(event, "Please specify a subcommand.");
			return;
		}

		String subCommandName = splitMessage[1];
		TextCommand subCommand = findSubCommand(subCommands.get(groupCommand), subCommandName);

		if (subCommand!=null && !(subCommand instanceof GroupCommand)) {
			subCommand.execute(CommandEvent.from(event), Arrays.copyOfRange(splitMessage, 2, splitMessage.length));
		} else if (subCommand != null) {
			GroupCommand subGroupCommand = (GroupCommand) subCommand;
			if (splitMessage.length == 2) {
				reply(event, "Please specify a subcommand.");
				return;
			}
			TextCommand subSubCommand = findSubCommand(subCommands.get(subGroupCommand), splitMessage[2]);
			if (subSubCommand == null) {
				reply(event, "Subcommand not found.");
				return;
			}

			subSubCommand.execute(CommandEvent.from(event), Arrays.copyOfRange(splitMessage, 3, splitMessage.length));
		}

	}

	private TextCommand findSubCommand(List<Command> commands, String subCommandName) {
		for (Command c : commands) {
			if (c.getName().equalsIgnoreCase(subCommandName)) {
				return (TextCommand) c;
			}
		}
		return null;
	}

	private void reply(MessageReceivedEvent event, String message) {
		event.getMessage().reply(message).queue();
	}

	public static Command isValid(String command) {
		return commands.stream().filter(cmd -> cmd.getName().equalsIgnoreCase(command)).findFirst()
				.orElseGet(() -> subCommands.keySet().stream().filter
						(cmd -> cmd.getName().equalsIgnoreCase(command)).findFirst().orElse(null));
	}
}
