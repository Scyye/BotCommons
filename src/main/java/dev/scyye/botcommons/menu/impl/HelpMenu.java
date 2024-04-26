package dev.scyye.botcommons.menu.impl;

import dev.scyye.botcommons.commands.*;
import dev.scyye.botcommons.menu.Menu;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.MarkdownUtil;

import java.util.*;

import static dev.scyye.botcommons.commands.CommandManager.getCommand;

@Command(name = "help", help = "Displays a help menu")
@Menu(id = "help-menu")
public class HelpMenu extends PageMenu implements ICommand {
	SelfUser me;
	Map<String, String> permissionStrings = new HashMap<>(){
		{
			put("owner", ":crown: Owner");
			put("admin", ":exclamation: Admin");
			put("mod", ":amongusdance: Moderator");
			put("access", ":ticket: Ticket");
			put("member", ":smiley: Member");
		}
	};

	public HelpMenu() {

	}

	public HelpMenu(SelfUser me) {
		this.me = me;
	}

	@Override
	public void handle(GenericCommandEvent event) {
		event.deferReply();
		ICommand command = getCommand(event.getArg("command", String.class));

		if (command == null) {
			// No command was specified, display help for all commands
			event.replyMenu("help-menu", event.getJDA().getSelfUser());
			return;
		}

		CommandInfo info = command.getCommandInfo();

		String usage = getUsage(info, event);

		EmbedBuilder embed = new EmbedBuilder()
				.setTitle(STR."Help for \{event.getArg("command", String.class)}")
				.setDescription(info.help)
				.addField("Usage", MarkdownUtil.codeblock(usage), false);

		if (info.aliases.length>0) {
			StringBuilder aliases = new StringBuilder();
			for (String alias : info.aliases) {
				aliases.append(MarkdownUtil.monospace(alias)).append("\n");
			}
			embed.addField("Aliases", aliases.toString(), false);
		}

		embed.setColor(0x00ff00);
		String footer = "";
		if (info.args.length>0) {
			footer += "Args in <> are required, args in [] are optional\nYou dont need to include `<>` or `[]` in the command.";
		}

		embed.setFooter(footer);

		event.replyEmbed(embed);
	}

	public static String getUsage(CommandInfo info, GenericCommandEvent event) {
		StringBuilder usage = new StringBuilder();

		// Append prefix and command name
		usage.append(event != null ? event.getPrefix():"=").append(info.name);

		String currentType = null;
		int consecutiveCount = 0;

		for (CommandInfo.Option option : info.args) {
			String type = option.type().toString(); // Assuming type is a string for simplicity

			if (currentType == null || !currentType.equals(type)) {
				// Append the current option
				appendOption(usage, option, event);

				// Reset consecutive count for the new type
				currentType = type;
				consecutiveCount = 0;
			} else {
				// Increment consecutive count
				consecutiveCount++;
			}

			if (consecutiveCount > 5) {
				// If consecutive count exceeds 5, append "[arg ...]" and reset count
				usage.append(" [").append(removeNumber(option.name())).append("...]");
				consecutiveCount = 0;
			}
		}

		return usage.toString();
	}

	private static String removeNumber(String str) {
		return str.replaceAll("[0-9]", "");
	}

	private static void appendOption(StringBuilder usage, CommandInfo.Option option, GenericCommandEvent event) {
		// Append the option name
		usage.append(" ").append(option.isRequired() ? "<" : "[").append(option.name()).append(option.isRequired() ? ">" : "");

		if (option.isRequired())
			return;

		// Append the default value if present
		if (event != null && option.defaultValue() != null) {
			usage.append(" = ").append(option.defaultValue().apply(event));
		}

		// Close the option bracket
		usage.append("]");
	}

	@Override
	public void handleAutocomplete(CommandAutoCompleteInteractionEvent event) {
		if (event.getFocusedOption().getName().equals("command")) {
			event.replyChoiceStrings(
					CommandManager.commands.stream().map(ICommand::getCommandInfo).map(commandInfo -> {
						return commandInfo.name;
					}).filter(
							name -> name.contains(event.getFocusedOption().getValue())
					).limit(25).toArray(String[]::new)
			).queue();
		}
	}

	@Override
	public CommandInfo.Option[] getArguments() {
		return new CommandInfo.Option[]{
				CommandInfo.Option.optional("command", "The command to get help for", OptionType.STRING, "none", true)
		};
	}

	@Override
	public List<EmbedBuilder> getPageData() {
		List<EmbedBuilder> result = new ArrayList<>();

		int commandsOnPage = 0;

		String[] categories = CommandManager.commands.stream().map(
				command -> command.getCommandInfo().category.replace("_", " ")
		).distinct().toArray(String[]::new);

		for (String category : categories) {
			if (Objects.equals(category, "HIDDEN"))
				continue;

			EmbedBuilder temp = new EmbedBuilder()
					.setAuthor(me.getName(), null, me.getAvatarUrl())
					.setTitle(category.replace("_", " "))
					.setFooter("Use /help [command] for a more in-depth description of any command");
			for (ICommand command : CommandManager.commands) {
				if (commandsOnPage == 5) {
					commandsOnPage = 0;
					result.add(temp);
					temp.clearFields();
				}

				if (Objects.equals(command.getCommandInfo().category, category)) {
					temp.addField(STR."\{MarkdownUtil.bold(command.getCommandInfo().name)} \{MarkdownUtil.monospace(getUsage(command.getCommandInfo(), null))}", command.getCommandInfo().help, false);
					commandsOnPage++;
				}
			}

			if (!temp.getFields().isEmpty())
				result.add(temp);
		}

		/*
		EmbedBuilder temp = new EmbedBuilder()
				.setAuthor(me.getName(), null, me.getAvatarUrl())
				.setFooter("Use =help [command] for a more in-depth description of any command");
		for (ICommand command : CommandManager.commands) {
			if (commandsOnPage > 6) {
				commandsOnPage = 0;
				result.add(temp.build());
				temp.clearFields();
			}

			temp.addField(MarkdownUtil.bold(command.getCommandInfo().name), command.getCommandInfo().help, false);

			commandsOnPage++;
		}
		result.add(temp.build());

		 */

		return result;
	}
}
