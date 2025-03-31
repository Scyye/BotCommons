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
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
@Experimental
public class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final String assetPath;
	@Getter
	private final Map<String, Config> configs = new ConcurrentHashMap<>();
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
		final int MAX_RETRIES = 3;
		int attempts = 0;
		boolean success = false;

		while (!success && attempts < MAX_RETRIES) {
			attempts++;
			try {
				Path configPath = Path.of(assetPath, "server-configs", serverId + ".json");
				Files.createDirectories(configPath.getParent());
				Files.writeString(configPath, GSON.toJson(config));
				success = true;
				System.out.println("[ConfigManager] Successfully wrote config for server " + serverId);
			} catch (IOException e) {
				String errorMsg = String.format("[ConfigManager] Failed to write config for server %s (Attempt %d/%d): %s",
						serverId, attempts, MAX_RETRIES, e.getMessage());
				System.err.println(errorMsg);

				if (attempts >= MAX_RETRIES) {
					System.err.println("[ConfigManager] Max retries reached for writing config. Operating with in-memory config only for server " + serverId);
					e.printStackTrace();
				} else {
					// Wait before retrying (with exponential backoff)
					try {
						Thread.sleep(500L * attempts);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						System.err.println("[ConfigManager] Interrupted while waiting to retry config write");
						break;
					}
				}
			}
		}

		// Ensure configs map is always updated regardless of file write success
		configs.put(serverId, config);
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
		private transient final Map<String, Type> configTypes = new HashMap<>();

		public void put(String key, Object value) {
			config.put(key, GSON.toJson(value));
			configTypes.put(key, value.getClass());
		}

		public <T> T get(String key, Class<T> type) {
			return GSON.fromJson(config.get(key), type);
		}

		public Object getObject(String key) {
			return GSON.fromJson(config.get(key), configTypes.get(key));
		}

		public boolean missingKey(String key) {
			return !config.containsKey(key);
		}

		public List<String> keySet() {
			return List.copyOf(config.keySet());
		}

		private static <T> T fromJson(String json, Class<T> type) {
			return type == String.class ? type.cast(json) : GSON.fromJson(json, type);
		}

		private static String stringify(Object value) {
			return value instanceof String ? (String) value : GSON.toJson(value);
		}
	}
}
