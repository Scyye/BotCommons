package botcommons.commands.impl;

import botcommons.config.GuildConfig;
import com.google.gson.GsonBuilder;
import botcommons.commands.GenericCommandEvent;
import botcommons.commands.AutoCompleteHandler;
import botcommons.commands.Command;
import botcommons.commands.CommandHolder;
import botcommons.commands.Param;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

import java.util.stream.Collectors;

@CommandHolder
public class ConfigCommand {

	@Command(name = "config", help = "Get or set a config value")
	public static void config(GenericCommandEvent event,
							  @Param(description = "The key to get or set", autocomplete = true) String key,
							  @Param(description = "The value to set", autocomplete = true) String value) {
		if (key == null && value == null) {
			event.replySuccess(new GsonBuilder().setPrettyPrinting().create().toJson(event.getConfig()));
			return;
		}

		if (value == null) {
			event.replySuccess("Value for key " + key + " is " +
					new GsonBuilder().setPrettyPrinting().create().toJson(event.getConfig().get(key)));
			return;
		}

		if (event.getConfig().set(key, value)) {
			Member member = event.getGuild().getMember(event.getUser());
			if (member == null) {
				event.replyError("Failed to get member.");
				return;
			}
			if (!member.hasPermission(Permission.MANAGE_SERVER)) {
				event.replyError("You do not have permission to set values.");
				return;
			}
			event.replySuccess("Set " + key + " to " + value);
			return;
		}
		event.replyError("Failed to set value, check logs.");
	}

	@AutoCompleteHandler("config")
	public static void handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
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
}
