package dev.scyye.botcommons.utilities;

import java.lang.reflect.Field;

@Deprecated(since = "1.7", forRemoval = true)
public class ClassUtilities {
	public static <T> T getFieldOfType(Object object, Class<T> type) {
		for (Field field : object.getClass().getDeclaredFields()) {
			if (field.getType().equals(type)) {
				field.setAccessible(true);
				return type.cast(field);
			}
		}
		return null;
	}
}
