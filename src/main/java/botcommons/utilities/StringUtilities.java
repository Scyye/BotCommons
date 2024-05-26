package botcommons.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

public class StringUtilities {
	@Deprecated(since = "1.7", forRemoval = true)
	public static String replaceSpecificCharacter(String string, char replacement, int index) {
		char[] chars = string.toCharArray();
		chars[index] = replacement;
		return new String(chars);
	}
	public static HashMap<String, String> stringifyMap(HashMap<?, ?> map) {
		HashMap<String, String> stringMap = new HashMap<>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		map.forEach((key, value) -> stringMap.put(gson.toJson(key), gson.toJson(value)));
		return stringMap;
	}
	@SuppressWarnings("unused")
	public static HashMap<?, ?> parseMap(HashMap<String, String> strings, Class<?> keyClass, Class<?> valueClass) {
		HashMap<Object, Object> map = new HashMap<>();
		Gson gson = new Gson();
		strings.forEach((key, value) -> map.put(gson.fromJson(key, keyClass), gson.fromJson(value, valueClass)));
		return map;
	}
}
