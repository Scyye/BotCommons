package botcommons.commands.impl;

import botcommons.commands.GenericCommandEvent;
import botcommons.commands.Command;
import botcommons.commands.CommandHolder;
import botcommons.commands.Param;
import botcommons.utilities.SQLiteUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.sql.ResultSet;

@SuppressWarnings("all")
@CommandHolder
@Deprecated(since = "1.11-config", forRemoval = true)
public class SQLCommand {

	@Command(name = "sql", help = "Run an SQL command")
	public static void sql(GenericCommandEvent event,
						   @Param(description = "The SQL command to run", type = OptionType.STRING) String sql) {
		boolean query = sql.toLowerCase().startsWith("select");
		if (!query) {
			if (!SQLiteUtils.execute(sql))
				event.replyError("Failed to execute command");
			else
				event.replySuccess("Command executed successfully");
			return;
		}
		try (ResultSet resultSet = SQLiteUtils.executeQuerySet(sql)) {
			// Convert the ResultSet into human-readable data
			StringBuilder result = new StringBuilder();
			if (resultSet == null) {
				event.replyError("No results found");
				return;
			}
			while (resultSet.next()) {
				for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
					result.append(resultSet.getMetaData().getColumnName(i)).append(": ").append(resultSet.getString(i)).append("\n");
				}
				result.append("\n");
			}
			if (!result.isEmpty())
				event.replySuccess(result.toString());
			else
				event.replySuccess("No results found");
		} catch (Exception e) {
			event.replyError(e.getMessage());
		}

	}
}
