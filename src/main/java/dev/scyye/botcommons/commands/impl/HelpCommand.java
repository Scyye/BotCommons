package dev.scyye.botcommons.commands.impl;

import dev.scyye.botcommons.commands.GenericCommandEvent;
import dev.scyye.botcommons.config.Config;
import dev.scyye.botcommons.config.GuildConfig;
import dev.scyye.botcommons.menu.Menu;
import dev.scyye.botcommons.menu.impl.PageMenu;
import dev.scyye.botcommons.methodcommands.MethodCommand;
import dev.scyye.botcommons.methodcommands.MethodCommandHolder;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.ArrayList;
import java.util.List;
/*
@Menu(id = "help-menu")
@MethodCommandHolder
public class HelpCommand extends PageMenu {
	private final List<EmbedBuilder> commands;

	public HelpCommand(List<EmbedBuilder> commands) {
		this.commands = commands;
	}

	public HelpCommand() {
		this.commands = new ArrayList<>();
	}

	@MethodCommand(name = "help", help = "Shows help")
	public static void help(GenericCommandEvent event) {
		List<String> validCommands = new ArrayList<>();
		List<String> validUsages = new ArrayList<>();
		List<String> validDescriptions = new ArrayList<>();

		GuildConfig config = event.getConfig();
		String prefix= config.get("prefix");

		for (var cmd : CommandManager.commands) {
			if (cmd.getCommandInfo().category.equals("admin") && !event.getUser().getId().equals(Config.ownerId)) {
				continue;
			}
			validCommands.add(cmd.getCommandInfo().name);
			validUsages.add(cmd.getCommandInfo().usage);
			validDescriptions.add(cmd.getCommandInfo().help);
		}
		for (int i = 0; i < validCommands.size(); i++) {
			EmbedBuilder embed = new EmbedBuilder()
					.setTitle("Help")
					.addField(prefix+validCommands.get(i), validDescriptions.get(i), true);

		}

		event.replyMenu("help-menu");
	}

	@Override
	public List<EmbedBuilder> getPageData() {
		return List.of();
	}
}
*/
