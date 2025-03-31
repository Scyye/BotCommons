package botcommons.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused", "ignore"})
public class StringUtilities {
	public static String botName = "default";
	static Logger logger = LoggerFactory.getLogger(StringUtilities.class);

	/**
	 * This method converts a HashMap with generic keys and values to a HashMap with String keys and String values by serializing each key and value to JSON format.
	 * @param map The input HashMap with generic keys and values. This map can contain any type of objects as keys and values.
	 * @return A new {@link HashMap} where each key and value from the input map has been serialized to a JSON string. This allows for easy storage or transmission of the map's contents in a standardized format.
	 */
	public static HashMap<String, String> stringifyMap(HashMap<?, ?> map) {
		HashMap<String, String> stringMap = new HashMap<>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		map.forEach((key, value) -> stringMap.put(gson.toJson(key), gson.toJson(value)));
		return stringMap;
	}

	/**
	 * This method parses a HashMap of String keys and String values back into a Map with specified key and value types using Gson for deserialization.
	 * @param strings The input HashMap with String keys and String values. This map is expected to contain JSON strings that represent the keys and values of the desired output map.
	 * @param keyClass The class type of the keys in the output map. This parameter specifies the type to which the keys in the input map should be deserialized. For example, if you want the keys to be integers, you would pass Integer.class.
	 * @param valueClass The class type of the values in the output map. This parameter specifies the type to which the values in the input map should be deserialized. For example, if you want the values to be strings, you would pass String.class or if you want them to be integers, you would pass Integer.class.
	 * @return A new {@link Map} where each key and value from the input HashMap has been deserialized into the specified types. The keys and values in the output map will be of the types specified by the keyClass and valueClass parameters, respectively. This allows for type-safe access to the elements in the resulting map.
	 * @param <K> The type of the keys in the output map. This is a generic type parameter that allows the method to return a map with keys of the specified type. For example, if you want the keys to be integers, you would specify Integer.class when calling this method.
	 * @param <V> The type of the values in the output map. This is a generic type parameter that allows the method to return a map with values of the specified type. For example, if you want the values to be strings, you would specify String.class when calling this method, or if you want them to be integers, you would specify Integer.class.
	 */
	@SuppressWarnings("unused")
	public static <K, V> Map<K, V> parseMap(HashMap<String, String> strings, Class<K> keyClass, Class<V> valueClass) {
		HashMap<K, V> map = new HashMap<>();
		Gson gson = new Gson();
		strings.forEach((key, value) -> map.put(gson.fromJson(key, keyClass), gson.fromJson(value, valueClass)));
		return map;
	}

	/**
	 * This method generates a path for an asset file based on the bot's name and the provided path. It ensures that the directories leading to the file exist, creating them if necessary.
	 * @param path The relative path to the asset file. This can be a subdirectory or a file name. The path will be appended to the bot's asset directory, which is named after the bot (e.g., "botName-assets").
	 * @return The full path to the asset file, which is constructed by combining the bot's asset directory with the provided path. This method ensures that the directories leading to the file exist, creating them if they do not. The returned path can be used for file operations such as reading or writing.
	 */
	public static Path getAssetPath(Path path) {
		// botName-assets is the default asset path, append the given path to it
		Path p = Path.of(botName + "-assets", path.toString());
		// Create the directories leading to the file if they don't exist
		File parentDir = p.toFile().getParentFile();
		if (!parentDir.exists()) {
			if (!parentDir.mkdirs()) {
				logger.warn("Failed to create directory: {}", parentDir.getAbsolutePath());
			} else {
				logger.debug("Created directory: {}", parentDir.getAbsolutePath());
			}
		}
		return p;
	}
}
