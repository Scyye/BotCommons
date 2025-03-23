package botcommons.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;

@SuppressWarnings({"unused", "ignore"})
public class StringUtilities {
	public static String botName = "null";
	static Logger logger = LoggerFactory.getLogger(StringUtilities.class);

	public static HashMap<String, String> stringifyMap(HashMap<?, ?> map) {
		HashMap<String, String> stringMap = new HashMap<>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		map.forEach((key, value) -> stringMap.put(gson.toJson(key), gson.toJson(value)));
		return stringMap;
	}
	@SuppressWarnings("unused")
	public static <T, V> HashMap<T, V> parseMap(HashMap<String, String> strings, Class<T> keyClass, Class<V> valueClass) {
		HashMap<T, V> map = new HashMap<>();
		Gson gson = new Gson();
		strings.forEach((key, value) -> map.put(gson.fromJson(key, keyClass), gson.fromJson(value, valueClass)));
		return map;
	}

	public static Path getAssetPath(Path path) {
		// botName-assets is the default asset path, append the given path to it
		Path p = Path.of(botName + "-assets", path.toString());
		// Create the directories leading to the file if they don't exist
		if (!p.toFile().getParentFile().mkdirs())
			logger.debug("Directory wasn't created, it probably already exists");
		return p;
	}
}
