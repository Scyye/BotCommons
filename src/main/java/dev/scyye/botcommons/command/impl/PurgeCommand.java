package dev.scyye.botcommons.command.impl;

import dev.scyye.botcommons.command.CommandEvent;
import dev.scyye.botcommons.command.TextCommand;

public class PurgeCommand implements TextCommand {
	@Override
	public String getName() {
		return "purge";
	}

	@Override
	public String getHelp() {
		return "Purges messages";
	}

	@Override
	public String getUsage() {
		return "!purge <number>";
	}

	@Override
	public void execute(CommandEvent event, String[] args) {
		if (args.length==0) {
			event.reply("NUMBER PLZ UWU");
		}
		int number = Integer.parseInt(args[0]) + 1;

		event.getChannel().getHistory().retrievePast(number).queue(messages -> {
			event.getChannel().purgeMessages(messages);
		});

		event.reply("Purged %s messages!", number).queue();
		event.getMessage().delete().queue();
	}
}
