package dev.scyye.botcommons.commands.impl;

import dev.scyye.botcommons.commands.Command;
import dev.scyye.botcommons.commands.CommandInfo;
import dev.scyye.botcommons.commands.GenericCommandEvent;
import dev.scyye.botcommons.commands.ICommand;
import dev.scyye.botcommons.utilities.SQLiteUtils;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.sql.ResultSet;

@Command(name = "sql", help = "Run a SQL command", aliases = {"s"}, permission = "admin", category = "OWNER")
public class SQLCommand implements ICommand {
	@Override
	public void handle(GenericCommandEvent event) {
		String sql = event.getArg("sql", String.class);

		try {
			boolean query = sql.toLowerCase().startsWith("select");
			if (query) {
				ResultSet resultSet = SQLiteUtils.executeQuerySet(sql);

				// Convert the ResultSet into human readable data
				StringBuilder result = new StringBuilder();
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

	@Override
	public CommandInfo.Option[] getArguments() {
		return new CommandInfo.Option[]{
				CommandInfo.Option.required("sql", "The SQL command to run", OptionType.STRING, false)
		};
	}
}
