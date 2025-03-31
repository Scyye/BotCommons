package botcommons.commands;

import botcommons.config.ConfigManager;
import botcommons.menu.MenuManager;
import botcommons.menu.types.BaseMenu;
import com.google.gson.Gson;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This class represents a generic command event for slash commands.
 * It wraps the {@link SlashCommandInteractionEvent} and provides additional utility methods
 * for handling command arguments and replies.
 */
@SuppressWarnings({"ConstantConditions", "unused"})
public class GenericCommandEvent {
	private final SlashCommandInteractionEvent slashCommandEvent;

	ReplyContext replyContext;

	private GenericCommandEvent(SlashCommandInteractionEvent slashCommandEvent) {
		this.slashCommandEvent = slashCommandEvent;
	}

	/**
	 * Creates a new instance of {@link GenericCommandEvent} from a {@link SlashCommandInteractionEvent}.
	 * @param event The {@link SlashCommandInteractionEvent} to wrap.
	 * @return A new instance of {@link GenericCommandEvent} that wraps the provided {@link SlashCommandInteractionEvent}.
	 */
	public static GenericCommandEvent of(@NotNull SlashCommandInteractionEvent event) {
		GenericCommandEvent e = new GenericCommandEvent(event);
		e.replyContext = new ReplyContext(event);
		return e;
	}

	/**
	 * @return Returns the {@link JDA} instance associated with this command event.
	 */
	public JDA getJDA() {
		return slashCommandEvent.getJDA();
	}

	/**
	 * @return Returns true if the command was invoked from a guild (server), false otherwise.
	 */
	public boolean isGuild() {
		return slashCommandEvent.isFromGuild();
	}

	/**
	 * @return Returns true if the command was invoked in an area the bot can't access
	 */
	public boolean isDetached() {
		return slashCommandEvent.getChannel().isDetached();
	}

	/**
	 * @return Returns the {@link Guild} associated with this command event, or null if the command was invoked in a private channel (DM).
	 */
	@Nullable
	public Guild getGuild() {
		return slashCommandEvent.getGuild();
	}

	/**
	 * @return Returns the ID of the guild associated with this command event.
	 * If the command was invoked in a private channel (DM), it returns "-1".
	 */
	public String getGuildId() {
		return isGuild()? getGuild().getId():"-1";
	}

	/**
	 * @return Returns the user who invoked the command.
	 */
	public User getUser() {
		return slashCommandEvent.getUser();
	}

	/**
	 * @return Returns the ID of the user who invoked the command.
	 */
	public String getUserId() {
		return getUser().getId();
	}

	/**
	 * @return Returns the member who invoked the command in a guild context, or null if the command was invoked in a private channel (DM).
	 */
	@Nullable
	public Member getMember() {
		return slashCommandEvent.getMember();
	}

	/**
	 * @return The {@link SlashCommandInteraction} associated with this command event.
	 */
	public SlashCommandInteraction getSlashCommandInteraction() {
		return slashCommandEvent.getInteraction();
	}

	/**
	 * @return Returns the {@link MessageChannel} where the command was invoked.
	 */
	public MessageChannel getChannel() {
		return slashCommandEvent.getChannel();
	}

	/**
	 * @return Returns the name of the command that was invoked.
	 * This is the name of the slash command as defined when it was registered.
	 */
	public String getCommandName() {
		return slashCommandEvent.getName();
	}

	/**
	 * @return Returns the name of the subcommand that was invoked, if applicable.
	 */
	public String getSubcommandName() {
		return slashCommandEvent.getSubcommandName();
	}

	/**
	 * @return Returns the name of the subcommand group that was invoked, if applicable.
	 * This is useful for commands that have subcommand groups.
	 */
	public String getSubcommandGroup() {
		return slashCommandEvent.getSubcommandGroup();
	}

	/**
	 * @deprecated Useless, text commands are no longer supported.
	 * @return Returns true if this command is a slash command. (always)
	 */
	@Deprecated(forRemoval = true)
	boolean isSlashCommand() {
		return true;
	}

	/**
	 * This method retrieves the arguments passed to the command in a structured way.
	 * @return An array of {@link Data} objects representing the options passed to the command.
	 */
	public Data[] getArgs() {
		return Arrays.stream(slashCommandEvent.getOptions().toArray(OptionMapping[]::new)).map(
				optionMapping -> {
					CommandInfo.Option option = CommandInfo.from(this).getOption(optionMapping.getName());
					var data = switch (option.getType()) {
						case UNKNOWN, SUB_COMMAND, SUB_COMMAND_GROUP -> null;
						case STRING -> optionMapping.getAsString();
						case INTEGER -> optionMapping.getAsLong();
						case BOOLEAN -> optionMapping.getAsBoolean();
						case USER -> optionMapping.getAsUser();
						case CHANNEL -> optionMapping.getAsChannel();
						case ROLE -> optionMapping.getAsRole();
						case MENTIONABLE -> optionMapping.getAsMentionable();
						case NUMBER -> optionMapping.getAsDouble();
						case ATTACHMENT -> optionMapping.getAsAttachment();
					};
					return new Data(option, data);
				}
		).toArray(Data[]::new);
	}

	public record Data(CommandInfo.Option option, Object value) {
	}


	/**
	 * This method retrieves the value of a specific argument by its name and casts it to the specified type.
	 * @param name The name of the argument to retrieve. This should match the name of the option as defined in the command registration.
	 * @param type The class type to which the argument value should be cast. This allows for type-safe retrieval of the argument value.
	 * @return The value of the argument cast to the specified type, or null if the argument is not present or cannot be cast to the specified type.
	 * @param <T> The type of the argument to retrieve. This is a generic type parameter that allows the method to return the value in the desired type.
	 */
	public <T> T getArg(String name, Class<T> type) {
		CommandInfo from = CommandInfo.from(this);
		CommandInfo.Option option = from.getOption(name);

		if (option == null) {
			return null;
		}

		for (Data arg : getArgs()) {
			if (arg.option.getName().equals(name)) {
				return type.cast(arg.value);
			}
		}

		if (option.isRequired()) {
			if (type==String.class)
				return type.cast(option.getDefaultValue());
			return new Gson().fromJson(option.getDefaultValue(), type);
		}

		return null;
	}

	/**
	 * @return Returns the {@link botcommons.config.ConfigManager.Config} associated with the current guild.
	 */
	public ConfigManager.Config getConfig() {
		return ConfigManager.getInstance().getConfigs().get(getGuildId());
	}

	public ReplyContext reply(String message) {
		return this.replyContext.content(message);
	}

	public void deferReply() {
		slashCommandEvent.deferReply().queue();
	}

	public boolean reply(String message, Consumer<Message> success) {
		return this.replyContext.content(message).finish(success);
	}

	public ReplyContext replySuccess(String message) {
		return this.replyContext.embed(new EmbedBuilder().setColor(0x00ff00).setDescription(message));
	}

	public ReplyContext replyError(String message) {
		return this.replyContext.embed(new EmbedBuilder().setColor(0xff0000).setDescription(message));
	}

	public ReplyContext reply(MessageCreateData message) {
		this.replyContext.content(message.getContent());
		message.getEmbeds().forEach(e -> this.replyContext.embed(new EmbedBuilder(e)));
		return this.replyContext;
	}

	public <T extends GenericEvent> ReplyContext replyListener(Class<T> eventType, Predicate<T> filter,
													   Function<T, Void> listener) {
		return replyContext.listenOnce(eventType, filter, listener);
	}

	public ReplyContext replyEphemeral(String message) {
		return this.replyContext.content(message).ephemeral();
	}

	public ReplyContext replyEmbed(EmbedBuilder embed) {
		return this.replyContext.embed(embed);
	}

	public ReplyContext replyMenu(String menuId, Object... args) {
		return this.replyContext.menu(menuId, args);
	}

	/**
	 * This method registers a menu with a specific ID and then returns the reply context for that menu.
	 * @param id The ID to register the menu with. This should be unique to avoid conflicts with other menus.
	 * @param menu The {@link BaseMenu} instance to register. This represents the menu that will be displayed to the user when they interact with the command.
	 * @param args Additional arguments to pass to the menu when it is invoked. This allows for dynamic content to be passed into the menu, such as user-specific data or command context.
	 * @return Returns the {@link ReplyContext} associated with the registered menu. This allows for further customization of the reply context, such as adding more options or handling user interactions with the menu.
	 */
	public ReplyContext replyMenu(String id, BaseMenu menu, Object... args) {
		MenuManager.registerMenuWithId(id+"-fake", menu);
		System.out.println("Registered menu with id: " + id);
		return this.replyContext.menu(id+"-fake", args);
	}
}
