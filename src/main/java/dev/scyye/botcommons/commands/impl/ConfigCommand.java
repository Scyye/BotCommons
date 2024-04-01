package dev.scyye.botcommons.commands.impl;

import com.google.gson.GsonBuilder;
import dev.scyye.botcommons.commands.Command;
import dev.scyye.botcommons.commands.CommandInfo;
import dev.scyye.botcommons.commands.GenericCommandEvent;
import dev.scyye.botcommons.commands.ICommand;
import dev.scyye.botcommons.config.GuildConfig;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.stream.Collectors;

@Command(name = "config", help = "Config command")
public class ConfigCommand implements ICommand {

	@Override
	public void handle(GenericCommandEvent event) {
		String key = event.getArg(0, String.class);
		Object value = event.getArg(1, Object.class);

		if (key == null && value == null) {
			event.replySuccess(new GsonBuilder().setPrettyPrinting().create().toJson(event.getConfig()));
			return;
		}

		if (value == null) {
			event.replySuccess(STR."Value for key \{key} is " +
					STR."\{new GsonBuilder().setPrettyPrinting().create().toJson(event.getConfig().get(key))}");
			return;
		}

		if (event.getConfig().set(key, value)) {
			event.replySuccess(STR."Set \{key} to \{value}");
			return;
		}
		event.replyError("Failed to set value, check logs.");
	}

	@Override
	public void handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
		if (event.getFocusedOption().getName().equals("key")) {
			event.replyChoiceStrings(GuildConfig.def.keySet().stream().filter(
					key -> key.contains(event.getFocusedOption().getValue())
			).limit(25).collect(Collectors.toList())).queue();
		} else if (event.getFocusedOption().getName().equals("value")) {
			event.replyChoiceStrings(GuildConfig.fromGuildId(event.getGuild().getId()).get(
					event.getOption("key").getAsString()
			)).queue();
		}
	}

	@Override
	public CommandInfo.Option[] getArguments() {
		return new CommandInfo.Option[]{
				CommandInfo.Option.optional("key", "The key to get or set", OptionType.STRING, true),
				CommandInfo.Option.optional("value", "The value to set", OptionType.STRING, true)
		};
	}
}
