package dev.scyye.botcommons.config;

import dev.scyye.botcommons.commands.impl.ConfigCommand;
import dev.scyye.botcommons.commands.impl.SQLCommand;
import dev.scyye.botcommons.methodcommands.MethodCommandManager;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ConfigManager extends ListenerAdapter {
	static boolean ready = false;
	GuildConfig def;
	public ConfigManager(HashMap<String, Object> config) {
		this.def=GuildConfig.fromHashMap(config);
		MethodCommandManager.addCommands(new ConfigCommand(), new SQLCommand());
		ready=true;
	}

	@Override
	public void onGuildReady(@NotNull GuildReadyEvent event) {
		GuildConfig.setDefault(def);
		GuildConfig.create(new HashMap<>(), event.getGuild().getId());
	}
}
