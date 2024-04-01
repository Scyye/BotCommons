package dev.scyye.botcommons.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

public interface ICommand {
	void handle(GenericCommandEvent event);
	default void handleAutocomplete(CommandAutoCompleteInteractionEvent event) {}
	default CommandInfo.Option[] getArguments() {
		return new CommandInfo.Option[0];
	}
	default CommandInfo getCommandInfo() {
		return CommandInfo.from(this);
	}
}
