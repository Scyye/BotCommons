package botcommons.commands.impl;

import botcommons.config.ConfigManager;
import botcommons.commands.GenericCommandEvent;
import botcommons.commands.AutoCompleteHandler;
import botcommons.commands.Command;
import botcommons.commands.CommandHolder;
import botcommons.commands.Param;
import botcommons.menu.Menu;
import botcommons.menu.types.BaseMenu;
import botcommons.menu.types.PageMenu;
import botcommons.menu.types.SelectMenu;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@CommandHolder
public class ConfigCommand {

    @Command(name = "config", help = "Get or set a config value", userContext = {InteractionContextType.GUILD})
    public static void config(GenericCommandEvent event,
                              @Param(description = "The key to get or set", autocomplete = true, required = false) String key,
                              @Param(description = "The value to set", autocomplete = true, required = false) String value) {
        ConfigManager configManager = ConfigManager.getInstance();
        String serverId = event.getGuildId();
        ConfigManager.Config config = configManager.getConfigs().getOrDefault(serverId, configManager.getConfigs().get("default"));

        if (key == null && value == null) {
            event.replySuccess(config.toString()).finish();
            return;
        }

        if (key == null) {
            event.replyError("Key is required.").finish();
            return;
        }

        if (value == null) {
            String configValue = config.get(key, String.class);
            event.replySuccess("Value for key " + key + " is " + configValue).finish();
		} else {
            if (config.missingKey(key)) {
                // If the key is not present, check if the user wants to create it
                if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                    event.replyError("You do not have permission to set values.").finish();
                    return;
                }
                event.replyMenu(
                        "yesno",
                        new SelectMenu() {
                            @Override
                            protected Option[] getOptions() {
                                return new Option[]{
                                        new Option("yes", (event) -> {
                                            System.out.println("Yes");
                                            config.put(key, value);
                                            configManager.setConfig(serverId, config);
                                            event.getHook().sendMessage("Set " + key + " to " + value).queue();
                                        }),
                                        new Option("no", (event) -> {
                                            System.out.println("No");
                                            event.getHook().sendMessage("Cancelled").setEphemeral(true).queue();
                                        })
                                };
                            }
                        }
                ).finish();

                return;
            }

            if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                event.replyError("You do not have permission to set values.").finish();
                return;
            }

            config.put(key, value);
            configManager.setConfig(serverId, config);
            event.replySuccess("Set " + key + " to " + value).finish();
        }
    }

    @AutoCompleteHandler("config")
    public static void handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
        if (!event.isFromGuild()) return;

        ConfigManager configManager = ConfigManager.getInstance();
        String serverId = event.getGuild().getId();
        ConfigManager.Config config = configManager.getConfigs().getOrDefault(serverId, configManager.getConfigs().get("default"));

        if (event.getFocusedOption().getName().equals("key")) {
            event.replyChoiceStrings(config.keySet().stream()
                    .filter(key -> key.contains(event.getFocusedOption().getValue()))
                    .limit(25)
                    .collect(Collectors.toList())).queue();
        } else if (event.getFocusedOption().getName().equals("value")) {
            if (event.getOption("key") == null) {
                event.replyChoiceStrings("Please select a key first").queue();
                return;
            }
            String key = event.getOption("key").getAsString();
            if (config.missingKey(key)) {
                event.replyChoiceStrings("Creating Key").queue();
                return;
            }
            String value = config.get(key, String.class);
            event.replyChoiceStrings(value).queue();
        }
    }
}
