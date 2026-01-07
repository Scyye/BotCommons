// java
// File: `src/main/java/botcommons/config/ConfigManager.java`

package botcommons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import jdk.jfr.Experimental;
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
	private final Map<String, Config> configs = new ConcurrentHashMap<>();
	private static ConfigManager instance;

	public static ConfigManager getInstance() {
		return instance;
	}

	public Map<String, Config> getConfigs() {
		return configs;
	}
	// java
// Replace constructor and setDefault with these versions

	public ConfigManager(String botName, JDA jda) {
		this.assetPath = botName + "-assets";
		jda.addEventListener(new ConfigListener());
		// Immediately load configs for any guilds JDA already knows about
		loadExistingGuildConfigs(jda);
		instance = this;
	}

	public void setDefault(Config config) {
		// persist the default to disk too
		setConfig("default", config);
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
				// Serialize the internal map as a JsonObject so values are real JSON values
				Files.writeString(configPath, GSON.toJson(config.toJsonObject()));
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

	private void loadExistingGuildConfigs(JDA jda) {
		// If JDA is already aware of guilds (e.g. was ready before listener added),
		// load each guild's config now so nothing is missed.
		for (var guild : jda.getGuilds()) {
			loadConfigForServer(guild.getId());
		}
	}

	private void loadConfigForServer(String serverId) {
		Path configPath = Path.of(assetPath, "server-configs", serverId + ".json");
		if (Files.exists(configPath)) {
			try {
				String json = Files.readString(configPath);
				var parsed = JsonParser.parseString(json);
				Config config;
				if (parsed.isJsonObject()) {
					config = Config.fromJsonObject(parsed.getAsJsonObject());
				} else {
					config = GSON.fromJson(json, Config.class);
				}
				configs.put(serverId, config);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JsonSyntaxException e) {
				System.err.println("[ConfigManager] Invalid JSON in config for server " + serverId);
				e.printStackTrace();
			}
		} else {
			configs.put(serverId, configs.get("default"));
		}
	}

	private class ConfigListener extends ListenerAdapter {
		@Override
		public void onGuildReady(@NotNull GuildReadyEvent event) {
			loadConfigForServer(event.getGuild().getId());
		}
	}

	public static class Config {
		private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
		private final Map<String, String> config = new HashMap<>();
		private transient final Map<String, Type> configTypes = new HashMap<>();

		public void remove(String key) {
			if (config.containsKey(key)) {
				config.remove(key);
				configTypes.remove(key);
			}
		}

		public void put(String key, Object value) {
			config.put(key, GSON.toJson(value));
			if (value != null) {
				configTypes.put(key, value.getClass());
			} else {
				configTypes.remove(key);
			}
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

		// Build a JsonObject from the internal map, parsing stored JSON-strings back into JsonElements
		public com.google.gson.JsonObject toJsonObject() {
			com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
			for (Map.Entry<String, String> entry : config.entrySet()) {
				String raw = entry.getValue();
				if (raw == null) {
					obj.add(entry.getKey(), com.google.gson.JsonNull.INSTANCE);
					continue;
				}
				try {
					com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(raw);
					obj.add(entry.getKey(), parsed);
				} catch (com.google.gson.JsonSyntaxException e) {
					// value was stored as a plain string that's not valid JSON; keep it as string
					obj.addProperty(entry.getKey(), raw);
				}
			}
			return obj;
		}

		// Reconstruct Config from a JsonObject, storing each value as a JSON string (matching how put(...) stores values)
		public static Config fromJsonObject(com.google.gson.JsonObject obj) {
			Config cfg = new Config();
			for (Map.Entry<String, com.google.gson.JsonElement> entry : obj.entrySet()) {
				com.google.gson.JsonElement el = entry.getValue();
				if (el == null || el.isJsonNull()) {
					cfg.config.put(entry.getKey(), null);
				} else {
					// store the element as its JSON string so existing get/put semantics work
					cfg.config.put(entry.getKey(), GSON.toJson(el));
				}
			}
			return cfg;
		}
	}
}
