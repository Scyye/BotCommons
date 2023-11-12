package dev.scyye.botcommons.config;

import com.google.gson.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.HashMap;

public class ServerConfig extends HashMap<String, Object> {
	static HashMap<String, ServerConfig> configs = new HashMap<>();
	static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	/**
	 * Creates a config file for the specified guild if it doesn't exist, otherwise returns the existing config
	 * @param guildId The id of the guild to create the config for
	 * @param defaultConfig The default config to use if the config doesn't exist
	 * @return The config for the specified guild
	 */
	public static ServerConfig createConfig(String guildId, HashMap<String, Object> defaultConfig) {
		String fileName = guildId + ".json";
		if (Files.exists(Path.of(fileName)))
			try {
				var c = configs.put(guildId, makeConfig(guildId, new File(fileName)));
				write(guildId);
				return c;
			} catch (IOException e) {
				e.printStackTrace();
			}

		var c = configs.put(guildId, makeConfig(guildId, defaultConfig));
		write(guildId);
		return c;
	}

	/**
	 * Do not use this unless you have to. Use {@link ServerConfig#createConfig(String, HashMap)} instead
	 * @param guildId The id of the guild to create the config for
	 * @param config The config options & values to use
	 * @return The config for the specified guild
	 */
	public static ServerConfig makeConfig(String guildId, HashMap<String, Object> config) {
		ServerConfig serverConfig = new ServerConfig();
		serverConfig.putAll(config);
		configs.put(guildId, serverConfig);
		return serverConfig;
	}

	/**
	 * Do not use this unless you have to. Use {@link ServerConfig#createConfig(String, HashMap)} instead
	 * @param guildId The id of the guild to create the config for
	 * @param file The file to read the config from
	 * @return The config for the specified guild
	 * @throws IOException If there's an issue with reading the file
	 */
	public static ServerConfig makeConfig(String guildId, File file) throws IOException {
		var serverConfig = gson.fromJson(Files.readString(file.toPath()), ServerConfig.class);
		configs.put(guildId, serverConfig);
		return serverConfig;
	}

	/**
	 * Writes the specified config to a file.
	 * Do not use this unless you first confirm that the config exists
	 * Use {@link ServerConfig#createConfig(String, HashMap)} instead
	 * @param guildId The id of the guild to write the config for
	 */
	public static void write(String guildId) {
		var config = configs.get(guildId);
		try {
			Files.writeString(
					new File(guildId + ".json").toPath(),
					gson.toJson(config));
		} catch (IOException e) {
			e.printStackTrace(new PrintWriter(System.err));
		}
	}


	/**
	 * Gets the value of the specified key as the specified type
	 * @param key A string key to get the value of from the config
	 * @param type The type of the value you're trying to get
	 * @return The value of the specified key as the specified type
	 * @param <T> The type of the value you're trying to get
	 */
	public <T> T get(String key, Class<T> type) {
		return type.cast(get(key));
	}

	/**
	 * PLEASE USE {@link #get(String, Class)} INSTEAD
	 * Override of {@link HashMap#get(Object)} to allow for getting values from the config
	 * @param key The key to get the value of from the config
	 * @return The value of the specified key
	 */
	@Override
	public Object get(Object key) {
		if (!(key instanceof String))
			throw new IllegalArgumentException("Key must be a string");
		if (!containsKey(key))
			return null;

		return get(key.toString(), String.class);
	}

	/**
	 * PLEASE USE {@link #get(String, Class)} INSTEAD
	 * Override of {@link HashMap#getOrDefault(Object, Object)} to allow for getting values from the config
	 * @param key the key whose associated value is to be returned
	 * @param defaultValue the default mapping of the key
	 * @return the value to which the specified key is mapped, or defaultValue if this map contains no mapping for the key
	 */
	@Override
	public Object getOrDefault(Object key, Object defaultValue) {
		if (!(key instanceof String))
			throw new IllegalArgumentException("Key must be a string");
		if (!containsKey(key))
			return defaultValue;
		return get(key.toString(), Object.class);
	}

	/**
	 * Uses Gson to convert the config to a json string
	 * @return A json representation of the config
	 */
	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this);
	}
}
