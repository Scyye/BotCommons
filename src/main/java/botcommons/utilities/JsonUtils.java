package botcommons.utilities;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.internal.utils.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
		try (FileWriter writer = new FileWriter(file)) {
			GSON.toJson(map, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void updateCache(HashMap<?, ?> map, String name) {
		File file = StringUtilities.getAssetPath(Path.of("cache/",name+".json")).toFile();
		HashMap<Object, Object> existingData = new HashMap<>();

		// Load existing data if the file exists
		if (file.exists()) {
			try (FileReader reader = new FileReader(file)) {
				Type type = new TypeToken<HashMap<?, ?>>() {}.getType();
				existingData.putAll(GSON.fromJson(reader, type));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Update existing data with new entries
		existingData.putAll(map);

		// Write updated data back to the file
		try (FileWriter writer = new FileWriter(file)) {
			// error safe json writing
			GSON.toJson(existingData, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
