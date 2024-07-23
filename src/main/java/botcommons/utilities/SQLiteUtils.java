package botcommons.utilities;

import botcommons.config.Config;
import botcommons.config.GuildConfig;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

// Suppressing all warnings is a bad practice, but considering this is a SQLite utility class, it's fine
@SuppressWarnings("all")
public class SQLiteUtils {
	static Connection connection = null;

	// Method to establish a connection to the database
	private static Connection connect() throws SQLException {
		Path path = Path.of("K:\\", "sqlite", Config.getInstance().get("bot-name")+".sqlite");
		// Define the database URL as a constant
		String DATABASE_URL = "jdbc:sqlite:" + path;
		try {
			if (!Files.exists(path.getParent()))
				Files.createDirectories(path.getParent());
			if (!Files.exists(path));
				Files.createFile(path);
		} catch (IOException ignored) {
		}

		if (connection == null || connection.isClosed()) {
			connection = DriverManager.getConnection(DATABASE_URL);
		}
		return connection;
	}

	public static boolean execute(String sql) {
		try (Connection connection = connect()) {
			Statement statement = connection.createStatement();
			statement.execute(sql);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean execute(String sql, String... args) {
		try (Connection connection = connect()) {
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				preparedStatement.setString(i + 1, args[i]);
			}
			return preparedStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static String jsonify(ResultSet set) {
		try {
			ResultSetMetaData md = set.getMetaData();
			int numCols = md.getColumnCount();
			List<String> colNames = IntStream.range(0, numCols)
					.mapToObj(i -> {
						try {
							return md.getColumnName(i + 1);
						} catch (SQLException e) {
							e.printStackTrace();
							return "?";
						}
					})
					.toList();

			JSONArray array = new JSONArray();
			while (set.next()) {
				JSONObject row = new JSONObject();
				colNames.forEach(cn -> {
					try {
						row.put(cn, set.getObject(cn));
					} catch (JSONException | SQLException e) {
						e.printStackTrace();
					}
				});
				array.put(row);
			}
			return array.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@NotNull
	public static HashMap<String, Object> executeQuery(String sql, String... args) {
		try (Connection connection = connect()) {
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				preparedStatement.setString(i + 1, args[i]);
			}
			ResultSet resultSet = preparedStatement.executeQuery();
			HashMap<String, Object> results = new HashMap<>();
			int i = 1;
			while (resultSet.next()) {
				results.put(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i));
				i++;
			}
			return results;
		} catch (SQLException e) {
			e.printStackTrace();
			return new HashMap<>();
		}
	}

	public static ResultSet executeQuerySet(String sql, String... args) {
		try {
			Connection connection = connect();
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			for (int i = 0; i < args.length; i++) {
				preparedStatement.setString(i + 1, args[i]);
			}
			return preparedStatement.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	// Method to insert data into the database
	public static void insertOrUpdateConfig(GuildConfig config) throws SQLException {
		// Using a PreparedStatement for parameterized queries
		Connection connection = connect();
		String sql = "INSERT OR REPLACE INTO config(";

		StringBuilder columns = new StringBuilder();
		StringBuilder values = new StringBuilder();
		for (String key : config.keySet()) {
			columns.append(key).append(", ");
			values.append("?, ");
		}
		columns.delete(columns.length() - 2, columns.length());
		values.delete(values.length() - 2, values.length());
		sql += columns + ") VALUES (" + values + ")";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		int i = 1;
		for (Object  value : config.values()) {
			preparedStatement.setObject(i, value);
			i++;
		}

		preparedStatement.executeUpdate();
	}

	public static void createCache(HashMap<?, ?> map, String name) {
		try (Connection connection = connect()) {
			String sql = "CREATE TABLE IF NOT EXISTS " + name + " (key TEXT PRIMARY KEY, value TEXT)";
			Statement statement = connection.createStatement();
			statement.execute(sql);
			for (Object key : map.keySet()) {
				sql = "INSERT INTO " + name + " (key, value) VALUES (?, ?)";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setObject(1, key);
				preparedStatement.setObject(2, map.get(key));
				preparedStatement.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void updateCache(HashMap<?, ?> map, String name) {
		map = StringUtilities.stringifyMap(map);
		try (Connection connection = connect()) {
			for (Object key : map.keySet()) {
				String sql = "INSERT OR REPLACE INTO " + name + " (key, value) VALUES (?, ?)";
				PreparedStatement preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setObject(1, key);
				preparedStatement.setObject(2, map.get(key));
				preparedStatement.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
