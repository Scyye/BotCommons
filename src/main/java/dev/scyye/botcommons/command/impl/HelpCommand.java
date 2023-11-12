package dev.scyye.botcommons.command.impl;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import dev.scyye.botcommons.command.*;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements TextCommand {

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public String getHelp() {
		return "Displays this menu.";
	}

	@Override
	public String getUsage() {
		return "!help [command]";
	}

	@Override
	public void execute(CommandEvent event, String[] args) {
		List<EmbedBuilder> builderList = new ArrayList<>();
		EmbedBuilder builder = new EmbedBuilder();
		builder
				.setAuthor("Scyye")
				.setColor(Color.CYAN);

		builderList.add(builder);

		if (args.length>0) {
			Command command = CommandManager.isValid(args[0]);
			if (command == null) {
				event.reply("Command not found. [`"+args[0]+"`]").queue();
				return;
			}

			builder.setTitle("Help for " + MarkdownUtil.monospace(command.getName()));
			builder.addField("Description", command.getHelp(), false);
			builder.addField("Usage", MarkdownUtil.monospace(command.getUsage()), false);
			event.getMessage().replyEmbeds(builder.build()).queue();
			return;
		}

		builder.setTitle("Help Menu");
		List<Command> everyCommand = new ArrayList<>(){{
			addAll(CommandManager.commands);
			addAll(CommandManager.subCommands.keySet());
		}};
		int i = 0;
		for (Command command : everyCommand) {
			if (i > 2) {
				i = 0;
				builderList.add(builder);
				builder = new EmbedBuilder();
				builder
						.setTitle("Help Menu")
						.setAuthor("Scyye")
						.setColor(Color.CYAN);
			}
			builder.addField(command.getName(), command.getHelp()+"\nUsage: `" +command.getUsage()+"`", false);
			i++;
		}

		event.replyMenu(builderList.stream().map(embedBuilder ->
				MessageCreateData.fromEmbeds(embedBuilder.build())).toArray(MessageCreateData[]::new));
	}
}
