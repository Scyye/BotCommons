package dev.scyye.botcommons.config;

import com.google.gson.GsonBuilder;
import dev.scyye.botcommons.utilities.ClassUtilities;
import net.dv8tion.jda.api.JDA;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Config extends HashMap<String, Object> {
	String botName;

	/**
	 * Creates a config file with the given values and bot name
	 * @param values The values to put in the config
	 * @param botName The name of the bot
	 * @return The {@link Config} object
	 */
	public static Config makeConfig(Map<String, Object> values, String botName) {
		String fileName = botName + ".json";
		if (Files.exists(Path.of(fileName))) {
			try {
				return makeConfig(fileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Config config = new Config();
		config.putAll(values);
		config.botName = botName;
		try {
			config.write();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return config;
	}

	public static Config makeConfig(String file) throws IOException {
		return new GsonBuilder().setPrettyPrinting().create()
				.fromJson(Files.readString(new File(file).toPath()), Config.class);
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(this);
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
		Files.writeString(new File(botName + ".json").toPath(), toString());
	}
}
