package dev.scyye.botcommons.commands.impl;

import dev.scyye.botcommons.commands.GenericCommandEvent;
import dev.scyye.botcommons.commands.Command;
import dev.scyye.botcommons.commands.CommandHolder;
import dev.scyye.botcommons.commands.Param;
import dev.scyye.botcommons.utilities.SQLiteUtils;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.sql.ResultSet;

@CommandHolder
public class SQLCommand {

	@Command(name = "sql", help = "Run an SQL command")
	public static void sql(GenericCommandEvent event,
						   @Param(description = "The SQL command to run", type = OptionType.STRING) String sql) {
		try {
			boolean query = sql.toLowerCase().startsWith("select");
			if (query) {
				ResultSet resultSet = SQLiteUtils.executeQuerySet(sql);

				// Convert the ResultSet into human readable data
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
				return;
			}

			SQLiteUtils.execute(sql);
			event.replySuccess("Command executed successfully");
		} catch (Exception e) {
			event.replyError(e.getMessage());
		}
	}
}
