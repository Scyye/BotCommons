package dev.scyye.botcommons.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface GroupCommand extends Command {
	String getName();
	default void execute(MessageReceivedEvent event, String[] args) {
		event.getMessage().reply("Please provide a valid subcommand.").queue();
	}
}
