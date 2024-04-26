package dev.scyye.botcommons.commands;

import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

/**
 * Use the new {@link dev.scyye.botcommons.methodcommands.MethodCommandManager} and other method command classes instead
 */
@Deprecated
@ForRemoval
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
