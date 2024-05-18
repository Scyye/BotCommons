package dev.scyye.botcommons.persist;

import dev.scyye.botcommons.utilities.SQLiteUtils;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PersistManager {
	// Save the objects and fields, along with the values, and keys defined in Persist.value()
	// a save function should be used to write the data to the database
	// a load function should be used to load the data from the database
	// All based off the keys, which should be unique and the primary key in the database

	public PersistManager() {
		SQLiteUtils.execute("CREATE TABLE IF NOT EXISTS persist (key TEXT PRIMARY KEY, value TEXT, field TEXT)");
	}

	public void save(Object object) {
		Class<?> clazz = object.getClass();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(Persist.class)) {
				Persist persist = field.getAnnotation(Persist.class);
				String key = persist.value();
				try {
					field.setAccessible(true);
					SQLiteUtils.execute("INSERT OR REPLACE INTO persist (key, value, field) VALUES (?, ?, ?)",
							key, field.get(object).toString(), field.getName());
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void load(Object object) {
		Class<?> clazz = object.getClass();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(Persist.class)) {
				Persist persist = field.getAnnotation(Persist.class);
				String key = persist.value();
				try {
					field.setAccessible(true);
					String value = SQLiteUtils.executeQuerySet("SELECT value FROM persist WHERE key = ?", key).getString("value");
					field.set(object, value);
				} catch (IllegalAccessException | SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
