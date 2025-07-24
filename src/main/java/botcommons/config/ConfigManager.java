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

	/**
	 * Initializes the ConfigManager with a default configuration.
	 * @param config The default configuration to be used if no specific server configuration is found.
	 */
	public void setDefault(Config config) {
		configs.put("default", config);
	}

	/**
	 * Sets the config for a server, and writes it to a file.
	 * @param serverId The ID of the server for which the configuration is being set. This will be used to create a unique file for the server.
	 * @param config The configuration object to be set for the server. This object will be serialized to JSON and saved to a file named after the server ID.
	 */
	public void setConfig(String serverId, Config config) {
		configs.put(serverId, config);
		writeConfigToFile(serverId, config);
	}

	/**
	 * Sets a specific value in the config for the server
	 * @param serverId The ID of the server for which the value is being set. This will determine which server's configuration will be updated.
	 * @param key The key under which the value will be stored in the configuration. This key will be used to retrieve the value later.
	 * @param value The value to be set in the configuration for the specified key. This can be any object, and it will be serialized to JSON when saved to the configuration file.
	 */
	public void setValue(String serverId, String key, Object value) {
		Config config = configs.getOrDefault(serverId, configs.get("default"));
		config.put(key, value);
		writeConfigToFile(serverId, config);
	}

	/**
	 * Retrieves a value from the configuration for a specific server.
	 * @param serverId The ID of the server for which the value is being retrieved. This will determine which server's configuration will be accessed.
	 * @param key The key for the value you want to retrieve from the configuration. This key should match the one used when setting the value.
	 * @param type The class type of the value you want to retrieve. This is used to deserialize the JSON value back into the appropriate Java type. For example, if you expect an Integer, you would pass Integer.class.
	 * @return The value associated with the specified key in the configuration for the given server. The return type will depend on the type parameter provided. If the key does not exist, it will return null.
	 * @param <T> The type of the value to be returned. This is a generic type parameter that allows the method to return the value in the desired type. For example, if you expect an Integer, you would specify Integer.class when calling this method.
	 */
	public <T> T getValue(String serverId, String key, Class<T> type) {
		Config config = configs.getOrDefault(serverId, configs.get("default"));
		return config.get(key, type);
	}

	/**
	 * Write the configuration to a file for a specific server.
	 * @param serverId The ID of the server for which the configuration is being written. This will be used to create a unique filename for the configuration file.
	 * @param config The configuration object to be written to the file. This object will be serialized to JSON and saved to a file named after the server ID.
	 */
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

			writeConfigToFile(serverId, configs.get(serverId));
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
