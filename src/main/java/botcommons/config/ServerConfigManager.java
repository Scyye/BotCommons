package botcommons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class ServerConfigManager {
	@Getter
	HashMap<String, ServerConfig> configs = new HashMap<>();

	String botName;
	String assetPath;
	JDA jda;

	@Getter
	static ServerConfigManager manager;

	public ServerConfigManager(String botName, JDA jda) {
		this.botName = botName;
		this.assetPath = botName + "-assets/";
		this.jda = jda;
		manager = this;
		jda.addEventListener(new ServerConfigListener());
	}

	public void setDefault(HashMap<String, Object> config) {
		ServerConfig serverConfig = new ServerConfig(config);
		configs.put("default", serverConfig);
	}

	public void setConfig(String serverId, HashMap<String, Object> config) {
		ServerConfig serverConfig = new ServerConfig(config);
		if (configs.put(serverId, serverConfig) == null)
			configs.put(serverId, configs.get("default"));

		write(serverId, configs.get(serverId));
	}

	public void setValue(String serverId, String key, Object value) {
		configs.get(serverId).putObject(key, value);
		write(serverId, configs.get(serverId));
	}

	public <T> T getValue(String serverId, String key, Type type) {
		return configs.get(serverId).getObject(key, type);
	}


	private void write(String serverId, HashMap<String, String> config) {
		// Write to file
		if (Files.notExists(Path.of(assetPath, "server-configs/"))) {
			try {
				Files.createDirectories(Path.of(assetPath, "server-configs/"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			Files.deleteIfExists(Path.of(assetPath, "server-configs/", serverId + ".json"));
				Files.writeString(Path.of(assetPath, "server-configs/", serverId + ".json"),
					new GsonBuilder().setPrettyPrinting().create().toJson(config));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeAll() {
		configs.forEach(this::write);
	}

	private static class ServerConfig extends HashMap<String, String> {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		public ServerConfig(HashMap<String, Object> config) {
			config.forEach(this::putObject);
		}

		public void putObject(String key, Object value) {
			if (value != null) {
				this.put(key, gson.toJson(value));
			}
		}

		public String put(String key, String value) {
			if (value != null) {
				super.put(key, value);
			}
			return value;
		}

		public String get(String key, Type type) {
			String json = this.get(key);
			return json != null ? gson.fromJson(json, type) : "Not Applicable";
		}
		public <T> T getObject(String key, Type type) {
			String json = this.get(key);
			return !json.equals("Not Applicable") ? gson.fromJson(json, type) : null;
		}

		public HashMap<String, Object> toMap() {
			HashMap<String, Object> map = new HashMap<>();
			this.forEach((key, value) -> map.put(key, gson.fromJson(value, Object.class)));
			return map;
		}
		public HashMap<String, String> toMapJson() {
			return new HashMap<>(this);
		}
	}

	public static class ServerConfigListener extends ListenerAdapter {
		@Override
		public void onGuildReady(@NotNull GuildReadyEvent event) {
			System.out.println(new Gson().toJson(getManager().configs.get("default")));
			if (getManager().configs.get("default") == null) {
				System.out.println("Default config is null");
				return;
			}
			// Load config
			if (Files.exists(Path.of(manager.assetPath, "server-configs/", event.getGuild().getId() + ".json"))) {
				try {
					ServerConfig config = new ServerConfig(new HashMap<>());
					//ServerConfig config = new Gson().fromJson(Files.readString(Path.of(manager.assetPath, "server-configs/", event.getGuild().getId() + ".json")), ServerConfig.class);
					manager.setConfig(event.getGuild().getId(), config.toMap());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println(getManager().configs.size());
				System.out.println(new Gson().toJson(getManager().configs.get("default")));
				manager.setConfig(event.getGuild().getId(), getManager().configs.get("default").toMap());
			}
		}
	}
}
