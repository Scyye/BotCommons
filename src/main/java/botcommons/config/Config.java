package botcommons.config;

import com.google.gson.GsonBuilder;
import lombok.Getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@SuppressWarnings("unused")
public class Config extends HashMap<String, Object> {
	@Getter
	private static Config instance;

	/**
	 * Creates a config file with the given values and bot name
	 * @param oldValues The values to put in the config
	 * @return The {@link Config} object
	 */
	// src/main/java/botcommons/config/Config.java
	public static Config makeConfig(Map<String, Object> oldValues, String botName) {
		Map<String, Object> values = new HashMap<>();
		values.put("bot-name", botName);
		values.put("token", "TOKEN");
		values.put("owner-id", "OWNER_ID");
		String fileName = botName + "-assets\\config.json";
		if (Files.exists(Path.of(fileName))) {
			try {
				instance = makeConfig(fileName);
				return instance;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		new File(botName + "-assets").mkdirs();
		Config config = new Config();
		// preserve object types instead of converting to JSON strings
		oldValues.forEach(values::put);
		config.putAll(values);
		instance = config;
		try {
			config.write();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instance;
	}


	public static Config makeConfig(String file) throws IOException {
		Config config = new GsonBuilder().setPrettyPrinting().create()
				.fromJson(Files.readString(new File(file).toPath()), Config.class);

		if (config.get("token").equalsIgnoreCase("TOKEN"))
			throw new IllegalArgumentException("Token not set in config file");

		if (config.get("owner-id").equalsIgnoreCase("OWNER_ID"))
			throw new IllegalArgumentException("Owner ID not set in config file");

		return config;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this);
	}

	public String get(String key) {
		return get(key, String.class);
	}

	public <T> T get(String key, Class<T> type) {
		return type.cast(super.get(key));
	}

	@Override
	public Object get(Object key) {
		if (!(key instanceof String))
			throw new IllegalArgumentException("Key must be a string");
		if (!containsKey(key))
			return null;

		return get(key.toString(), String.class);
	}

	public void write() throws IOException {
		Files.writeString(Path.of(Config.instance.get("bot-name") + "-assets", "config.json"), toString());
	}
}
