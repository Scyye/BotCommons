package dev.scyye.botcommons.command.impl;

import dev.scyye.botcommons.command.*;
import dev.scyye.botcommons.utilities.StringUtilities;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Map;

public class TreeCommand implements TextCommand {
	@Override
	public String getName() {
		return "tree";
	}

	@Override
	public String getHelp() {
		return "Displays a tree for commands with sub commands.";
	}

	@Override
	public String getUsage() {
		return "!tree <command>";
	}

	@Override
	public void execute(CommandEvent event, String[] args) {
		String display = """
				Macro:
				```
				↪ add <input> <output>
				↪ remove <macro>
				↪ edit
				   ↪ input <macro> <new input>
				   ↪ output <macro> <new output>
				↪ list
				↪ info
				```
				""";
		if (args.length==0) {
			event.getMessage().reply("Please specify a command.").queue();
			return;
		}

		Map.Entry<GroupCommand, List<Command>> group = null;

		for (var cmd : CommandManager.commands) {
			System.out.println(cmd.getName());
			if (!cmd.getName().equals(args[0]))
				continue;
			if (!(cmd instanceof GroupCommand groupCommand)) {
				event.getMessage().reply("Command has no sub commands, it is recommended to do `!help " + cmd.getName()+"`").queue();
				return;
			}

			group = Map.entry(groupCommand, CommandManager.subCommands.get(groupCommand));
		};


		if (group==null) {
			event.getMessage().reply("Command not found.").queue();
			return;
		}

		StringBuilder tree = new StringBuilder(StringUtilities.replaceSpecificCharacter(group.getKey().getName(),
				String.valueOf(group.getKey().getName().toCharArray()[0]).toUpperCase().charAt(0), 0));

		tree
				.append("\n```\n");
		for (var sub : group.getValue()) {
			System.out.println(sub.getName());

			tree
					.append("↪ ")
					.append(sub.getUsage())
					.append("\n");

			if (sub instanceof GroupCommand groupCommand) {
				System.out.println("Group command found: " + groupCommand.getName());
				if (CommandManager.subCommands.get(groupCommand)==null) {
					System.out.println("No sub commands found for " + groupCommand.getName());
					continue;
				}

				for (var subSub : CommandManager.subCommands.get(groupCommand))
					tree
							.append("\t↪ ")
							.append(subSub.getUsage())
							.append("\n");
			}



		}

		tree.append("```");


		event.getMessage().reply(tree.toString()).queue();
	}
}
