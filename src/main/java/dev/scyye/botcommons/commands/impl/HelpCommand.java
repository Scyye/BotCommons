package dev.scyye.botcommons.commands.impl;

import dev.scyye.botcommons.commands.Command;
import dev.scyye.botcommons.commands.CommandManager;
import dev.scyye.botcommons.commands.GenericCommandEvent;
import dev.scyye.botcommons.commands.ICommand;
import dev.scyye.botcommons.config.Config;
import dev.scyye.botcommons.config.GuildConfig;
import dev.scyye.botcommons.menu.Menu;
import dev.scyye.botcommons.menu.impl.PageMenu;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Menu(id = "help-menu")
@Command(name = "help", help = "Get help on commands", category = "GENERAL")
public class HelpCommand extends PageMenu implements ICommand {
	private final List<EmbedBuilder> commands;

	public HelpCommand(List<EmbedBuilder> commands) {
		this.commands = commands;
	}

	public HelpCommand() {
		this.commands = new ArrayList<>();
	}

	@Override
	public void handle(GenericCommandEvent event) {
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
		return null;
	}
}
