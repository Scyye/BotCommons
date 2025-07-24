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
	 * Creates a new Config instance from the provided old values and bot name.
	 * @param oldValues A map containing old values to be used in the configuration. This can include key-value pairs that will be added to the new config.
	 * @param botName The name of the bot for which the configuration is being created. This will be used to set the "bot-name" key in the configuration.
	 * @return Returns a new instance of the Config class, which is a HashMap containing the provided old values and additional default values.
	 */
	public static Config makeConfig(Map<String, Object> oldValues, String botName) {
		HashMap<String, String> values = new HashMap<>();
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
		new File(botName+"-assets").mkdirs();
		Config config = new Config();
		oldValues.forEach((key, value) -> {
			if (value instanceof String)
				values.put(key, (String) value);
			else
				values.put(key, new GsonBuilder().setPrettyPrinting().create().toJson(value));
		});
		config.putAll(values);
		instance = config;
		try {
			config.write();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instance;
	}

	/**
	 * Creates a new Config instance from a JSON file.
	 * @param file The path to the JSON file from which to load the configuration. This file should contain a valid JSON representation of a Config object.
	 * @return Returns a Config object that has been populated with the data from the specified JSON file. This object will be an instance of the Config class, which extends HashMap<String, Object>.
	 * @throws IOException If an I/O error occurs while reading the file. This can happen if the file does not exist, is not accessible, or if there are issues with reading the file's contents.
	 */
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

	/**
	 * Writes the current configuration to a JSON file.
	 * @throws IOException If an I/O error occurs while writing to the file. This can happen if the file is not writable, the directory does not exist, or there are permission issues.
	 */
	public void write() throws IOException {
		Files.writeString(Path.of(Config.instance.get("bot-name") + "-assets", "config.json"), toString());
	}
}
