package botcommons.commands;

import botcommons.config.GuildConfig;
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

// Ignore possible null issues
@SuppressWarnings({"ConstantConditions", "unused"})
public class GenericCommandEvent {
	private final SlashCommandInteractionEvent slashCommandEvent;

	ReplyContext replyContext;

	private GenericCommandEvent(SlashCommandInteractionEvent slashCommandEvent) {
		this.slashCommandEvent = slashCommandEvent;
	}

	public static GenericCommandEvent of(@NotNull SlashCommandInteractionEvent event) {
		GenericCommandEvent e = new GenericCommandEvent(event);
		e.replyContext = new ReplyContext(event);
		return e;
	}

	public JDA getJDA() {
		return slashCommandEvent.getJDA();
	}

	public boolean isGuild() {
		return slashCommandEvent.isFromGuild();
	}

	@Nullable
	public Guild getGuild() {
		return slashCommandEvent.getGuild();
	}

	public String getGuildId() {
		return isGuild()? getGuild().getId():"-1";
	}

	public User getUser() {
		return slashCommandEvent.getUser();
	}

	public String getUserId() {
		return getUser().getId();
	}

	@Nullable
	public Member getMember() {
		return slashCommandEvent.getMember();
	}

	public SlashCommandInteraction getSlashCommandInteraction() {
		return slashCommandEvent.getInteraction();
	}

	public MessageChannel getChannel() {
		return slashCommandEvent.getChannel();
	}

	public String getCommandName() {
		return slashCommandEvent.getName();
	}

	public String getSubcommandName() {
		return slashCommandEvent.getSubcommandName();
	}

	public String getSubcommandGroup() {
		return slashCommandEvent.getSubcommandGroup();
	}

	boolean isSlashCommand() {
		return true;
	}

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
	 * @deprecated Use {@link #getArg(String, Class)} instead
	 *
	 * @param index The index of the argument
	 * @return The {@link Data} of the argument
	 */
	@Deprecated(since = "1.7-commands", forRemoval = true)
	public Data getArg(int index) {
		return getArgs()[index];
	}


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

	public GuildConfig getConfig() {
		return GuildConfig.fromGuildId(getGuildId());
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
}
