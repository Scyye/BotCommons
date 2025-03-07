package botcommons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import botcommons.utilities.SQLiteUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;

/**
 * Deprecated since 1.11-config,  instead
 */
@Deprecated(since = "1.11-config", forRemoval = true)
public class GuildConfig extends HashMap<String, Object> {
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static GuildConfig def;

	private GuildConfig() {
		super();
	}

	public static void setDefault(HashMap<String, Object> config) {
		def = new GuildConfig();
		def.putAll(config);
		def.put("guild", "0");
	}

	public static void init(JDA jda) {
		if (def == null)
			setDefault(new GuildConfig());

		jda.addEventListener(new GuildConfigListener());
	}

	/**
	 * Creates a new GuildConfig object with the given values and guild id
	 * @deprecated since 1.7-config, use {@link #setDefault(HashMap)} and {@link #init(JDA)} instead
	 * @param values The values to put in the config
	 * @param guildId The guild id
	 * @return The {@link GuildConfig} object
	 */
	@Deprecated(since = "1.7-config", forRemoval = true)
	public static GuildConfig create(HashMap<String, Object> values, String guildId) {
		String sql =
				"CREATE TABLE IF NOT EXISTS config(guild TEXT NOT NULL PRIMARY KEY, " +
						String.join(", ", def.keySet().stream().filter(key ->
										!key.equals("guild")).map(key -> key + " TEXT")
								.toArray(String[]::new)) + ")";
		if (!SQLiteUtils.execute(sql)) {
			throw new IllegalStateException("Failed to create table");
		}
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
		} catch (SQLException ignored) {

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

		assert set != null;
		HashMap<String, Object> configMap = SQLiteUtils.resultSetToMap(set);
		return fromHashMap(configMap);
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
		Object value = super.get(key);
		if (type.isInstance(value)) {
			return type.cast(value);
		}
		return null;
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

	private static class GuildConfigListener extends ListenerAdapter {
		@Override
		public void onGuildReady(GuildReadyEvent event) {
			GuildConfig.create(GuildConfig.def, event.getGuild().getId());
		}
	}
}
