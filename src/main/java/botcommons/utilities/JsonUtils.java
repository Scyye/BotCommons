package botcommons.utilities;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;

public class JsonUtils {
	private static final Gson GSON = new GsonBuilder()
			.setExclusionStrategies(new ExclusionStrategy() {
				@Override
				public boolean shouldSkipField(FieldAttributes f) {
					// check if the field is accessable
					return !f.hasModifier(Modifier.PRIVATE) || f.getDeclaredType() == JDA.class ||
							f.getDeclaredClass() == Member.class ;
				}

				@Override
				public boolean shouldSkipClass(Class<?> clazz) {
					return false;
				}
			})
			.setPrettyPrinting()
			.create();


	public static <T, V> void createCache(HashMap<T, V> map, String name) {
		File file = StringUtilities.getAssetPath(Path.of("cache/",name+".json")).toFile();
		if (file.exists()) return;
		try (FileWriter writer = new FileWriter(file)) {
			GSON.toJson(map, writer);
		} catch (IOException e) {
			Logger logger = LoggerFactory.getLogger(JsonUtils.class);
			logger.error("Failed to create cache file for {}: {}", name, e.getMessage(), e);
		}
	}

	public static void updateCache(HashMap<?, ?> map, String name) {
		Logger logger = LoggerFactory.getLogger(JsonUtils.class);
		File file = StringUtilities.getAssetPath(Path.of("cache/", name + ".json")).toFile();
		HashMap<Object, Object> existingData = new HashMap<>();

		// Load existing data if the file exists
		if (file.exists()) {
			try (FileReader reader = new FileReader(file)) {
				Type type = new TypeToken<HashMap<?, ?>>() {}.getType();
				existingData.putAll(GSON.fromJson(reader, type));
                logger.debug("Loaded existing data from {}", file.getName());
			} catch (IOException e) {
                logger.error("Failed to read cache file {}: {}", file.getName(), e.getMessage(), e);
                return; // Avoid writing corrupted data
			}
		}

		// Update existing data with new entries
		existingData.putAll(map);

		// Write updated data back to the file
        synchronized (JsonUtils.class) { // Basic synchronization for thread safety
			try (FileWriter writer = new FileWriter(file)) {
				// error safe json writing
				GSON.toJson(existingData, writer);
                logger.debug("Updated cache file {}", file.getName());
			} catch (IOException e) {
                logger.error("Failed to write cache file {}: {}", file.getName(), e.getMessage(), e);
			}
	    }
	}
}
