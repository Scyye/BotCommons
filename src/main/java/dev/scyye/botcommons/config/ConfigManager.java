package dev.scyye.botcommons.config;

import dev.scyye.botcommons.methodcommands.CommandManager;
import dev.scyye.botcommons.methodcommands.impl.ConfigCommand;
import dev.scyye.botcommons.methodcommands.impl.SQLCommand;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@Deprecated(since = "1.7-config", forRemoval = true)
@SuppressWarnings("unused")
public class ConfigManager extends ListenerAdapter {
	static boolean ready = false;
	GuildConfig def;
	public ConfigManager(HashMap<String, Object> config) {
		this.def=GuildConfig.fromHashMap(config);
		CommandManager.addCommands(ConfigCommand.class, SQLCommand.class);
		ready=true;
	}

	@Override
	public void onGuildReady(@NotNull GuildReadyEvent event) {
		GuildConfig.setDefault(def);
		GuildConfig.create(new HashMap<>(), event.getGuild().getId());
	}
}
