package botcommons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jdk.jfr.Experimental;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Experimental
public class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final String assetPath;
	@Getter
	private final Map<String, Config> configs = new HashMap<>();
	@Getter
	private static ConfigManager instance;

	public ConfigManager(String botName, JDA jda) {
		this.assetPath = botName + "-assets/";
		jda.addEventListener(new ConfigListener());
		instance = this;
	}

	public void setDefault(Config config) {
		configs.put("default", config);
	}

	public void setConfig(String serverId, Config config) {
		configs.put(serverId, config);
		writeConfigToFile(serverId, config);
	}

	public void setValue(String serverId, String key, Object value) {
		Config config = configs.getOrDefault(serverId, configs.get("default"));
		config.put(key, value);
		writeConfigToFile(serverId, config);
	}

	public <T> T getValue(String serverId, String key, Class<T> type) {
		Config config = configs.getOrDefault(serverId, configs.get("default"));
		return config.get(key, type);
	}

	private void writeConfigToFile(String serverId, Config config) {
		try {
			Path configPath = Path.of(assetPath, "server-configs", serverId + ".json");
			Files.createDirectories(configPath.getParent());
			Files.writeString(configPath, GSON.toJson(config));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class ConfigListener extends ListenerAdapter {
		@Override
		public void onGuildReady(@NotNull GuildReadyEvent event) {
			String serverId = event.getGuild().getId();
			Path configPath = Path.of(assetPath, "server-configs", serverId + ".json");
			if (Files.exists(configPath)) {
				try {
					String json = Files.readString(configPath);
					Config config = GSON.fromJson(json, Config.class);
					configs.put(serverId, config);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				configs.put(serverId, configs.get("default"));
			}
		}
	}

	public static class Config {
		private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
		private final Map<String, String> config = new HashMap<>();

		public void put(String key, Object value) {
			if (value != null) {
				config.put(key, GSON.toJson(value));
			}
		}

		public <T> T get(String key, Class<T> type) {
			String json = config.get(key);
			return json != null ? GSON.fromJson(json, type) : null;
		}

		public <T> T get(String key, Type type) {
			String json = config.get(key);
			return json != null ? GSON.fromJson(json, type) : null;
		}

		public boolean missingKey(String key) {
			return !config.containsKey(key);
		}

		public List<String> keySet() {
			return List.copyOf(config.keySet());
		}

		@Override
		public String toString() {
			return GSON.toJson(this.config);
		}
	}
}
