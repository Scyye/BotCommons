package dev.scyye.botcommons.command.impl;

import dev.scyye.botcommons.command.CommandEvent;
import dev.scyye.botcommons.command.TextCommand;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class TestCommand implements TextCommand {
	@Override
	public String getName() {
		return "test";
	}

	@Override
	public String getHelp() {
		return "Test command for testing shit obviously";
	}

	@Override
	public String getUsage() {
		return "!test";
	}

	@Override
	public void execute(CommandEvent event, String[] args) {
		event.replyMenu(MessageCreateData.fromContent("This is page 1"), MessageCreateData.fromContent("This is page 2"));
	}
}
