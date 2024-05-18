package dev.scyye.botcommons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.scyye.botcommons.utilities.SQLiteUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;

public class GuildConfig extends HashMap<String, Object> {
	/**
	 * Config values required:
	 * create table if not exists config
	 * (
	 *     guild text not null,
	 *     category text not null,
	 *     log text not null,
	 *     greeting text,
	 *     closing text,
	 *     prefix text default '=' not null,
	 *     access text default '[]' not null,
	 *     ping text default '[]' not null,
	 *     snippets text default '[]' not null,
	 *     logging int default 0 not null,
	 *     blacklist text default '[]' not null,
	 *     roleBlacklist text default '[]' not null,
	 *     disabled int default 0 not null,
	 *     disabledReason text default 'No reason provided' not null,
	 *     anonymous int default 0 not null,
	 *     primary key (guild)
	 * );
	 */
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();


	public static GuildConfig def;

	public static void setDefault(GuildConfig config) {
		def = config;
		def.put("guild", "0");
	}

	public static GuildConfig create(HashMap<String, Object> values, String guildId) {
		//System.out.println(gson.toJson(def));
		String sql =
				"CREATE TABLE IF NOT EXISTS config(guild TEXT NOT NULL PRIMARY KEY, " +
						String.join(", ", def.keySet().stream().filter(key ->
										!key.equals("guild")).map(key -> key + " TEXT")
								.toArray(String[]::new));
		SQLiteUtils.execute(sql);
		GuildConfig config = new GuildConfig();
		for (Entry<String, Object> entry : def.entrySet()) {
			if (entry.getKey().equals("guild")){
				config.put(entry.getKey(), guildId);
				continue;
			}
			config.put(entry.getKey(), values.getOrDefault(entry.getKey(), entry.getValue()));
		}
		try	{
			if (SQLiteUtils.executeQuery("SELECT * FROM config WHERE guild = ?", guildId).isEmpty())
				SQLiteUtils.insertOrUpdateConfig(config);
		} catch (SQLException e) {
			System.out.println("error uwu");
		}


		return config;
	}

	@Override
	public String toString() {
		return gson.toJson(this);
	}

	public static boolean write(GuildConfig newConf) {
		try {
			SQLiteUtils.insertOrUpdateConfig(newConf);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static GuildConfig fromGuildId(String guildId) {
		if (Objects.equals(guildId, "-1"))
			return def;

		ResultSet set = SQLiteUtils.executeQuerySet("SELECT * FROM config WHERE guild = ?", guildId);

		var	json = SQLiteUtils.jsonify(set);
		assert json != null;
		json = json.substring(1, json.length()-1);

		//System.out.println(json);

		return gson.fromJson(json, GuildConfig.class);
		/*
		System.out.println(STR."Getting config for guild \{guildId}");


		try {
			assert set != null;
			System.out.println(STR."Size \{set.getFetchSize()}");
			GuildConfig res = new GuildConfig();
			int i = 1;
			while (set.next()) {
				res.put(set.getMetaData().getColumnName(i), set.getObject(i));
			}
			System.out.println(res.keySet().size());
			System.out.println(gson.toJson(res));
			return res;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

		 */
	}

	public static GuildConfig fromHashMap(HashMap<String, Object> map) {
		GuildConfig result = new GuildConfig();
		result.putAll(map);
		return result;
	}

	public boolean set(String key, Object value) {
		put(key, value);
		return write(this);
	}

	public <T> T get(String key, Class<T> type) {
		return type.cast(super.get(key));
	}

	public String get(String key) {
		return get(key, String.class);
	}

	@Override
	public Object get(Object key) {
		if (!(key instanceof String))
			throw new IllegalArgumentException("Key must be a string");
		if (!containsKey(key))
			return null;

		return get(key.toString(), String.class);
	}
}
