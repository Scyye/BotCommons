package dev.scyye.botcommons.persist;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class PersistManager {
	HashMap<Object, List<Field>> persistObjects;

	public void init(Object... objects) {
		persistObjects = new HashMap<>();
		for (var object : objects) {
			var fields = object.getClass().getDeclaredFields();
			persistObjects.put(object, List.of(fields));
		}
	}

	public void save() throws IllegalAccessException {
		for (var entry : persistObjects.entrySet()) {
			var object = entry.getKey();
			var fields = entry.getValue();

			for (var field : fields) {
				if (field.isAnnotationPresent(Persist.class)) {
					field.setAccessible(true);
					Object val = field.get(object);
					System.out.println(val);
				}
			}
		}
	}

	public void load() {
	}
}
