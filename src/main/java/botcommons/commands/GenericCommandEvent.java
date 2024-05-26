package botcommons.commands;

import botcommons.config.GuildConfig;
import com.google.gson.Gson;
import botcommons.cache.CacheManager;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

// Ignore possible null issues
@SuppressWarnings({"ConstantConditions", "unused"})
public class GenericCommandEvent {

	public enum Type {
		SLASH_COMMAND,
		MESSAGE_COMMAND
	}

	@Getter
	private final Type type;
	private final SlashCommandInteractionEvent slashCommandEvent;
	private final MessageReceivedEvent messageReceivedEvent;

	ReplyContext replyContext;

	private GenericCommandEvent(Type type, SlashCommandInteractionEvent slashCommandEvent, MessageReceivedEvent messageReceivedEvent) {
		this.type = type;
		this.slashCommandEvent = slashCommandEvent;
		this.messageReceivedEvent = messageReceivedEvent;
	}

	public static GenericCommandEvent of(@NotNull SlashCommandInteractionEvent event) {
		GenericCommandEvent e = new GenericCommandEvent(Type.SLASH_COMMAND, event, null);
		e.replyContext = new ReplyContext(event);
		return e;
	}

	public static GenericCommandEvent of(@NotNull MessageReceivedEvent event) {
		GenericCommandEvent e = new GenericCommandEvent(Type.MESSAGE_COMMAND, null, event);
		e.replyContext = new ReplyContext(event);
		return e;
	}

	public JDA getJDA() {
		return isSlashCommand() ? slashCommandEvent.getJDA() : messageReceivedEvent.getJDA();
	}

	public Message getMessage() {
		return isSlashCommand() ? null : messageReceivedEvent.getMessage();
	}

	public boolean isGuild() {
		return isSlashCommand() ? slashCommandEvent.isFromGuild() : messageReceivedEvent.isFromGuild();
	}

	public Guild getGuild() {
		return isSlashCommand() ? slashCommandEvent.getGuild() : messageReceivedEvent.getGuild();
	}

	public String getPrefix() {
		return GuildConfig.fromGuildId(getGuildId()).get("prefix");
	}

	public String getGuildId() {
		return isGuild()? getGuild().getId():"-1";
	}

	public User getUser() {
		return isSlashCommand() ? slashCommandEvent.getUser() : messageReceivedEvent.getAuthor();
	}

	public String getUserId() {
		return getUser().getId();
	}

	public Member getMember() {
		return isSlashCommand() ? slashCommandEvent.getMember() : messageReceivedEvent.getMember();
	}

	public SlashCommandInteraction getSlashCommandInteraction() {
		return isSlashCommand() ? slashCommandEvent.getInteraction() : null;
	}

	public MessageChannel getChannel() {
		return isSlashCommand() ? slashCommandEvent.getChannel() : messageReceivedEvent.getChannel();
	}

	public String getCommandName() {
		if (isSlashCommand()) {
			return slashCommandEvent.getName();
		}

		String result = messageReceivedEvent.getMessage().getContentRaw().replaceFirst(
				getJDA().getSelfUser().getAsMention() + " ", ""
		);
		if (result.startsWith(getPrefix())) {
			result = result.substring(getPrefix().length());
		}
		return result.split(" ")[0];
	}

	public String getSubcommandName() {
		if (isSlashCommand()) {
			return slashCommandEvent.getSubcommandName();
		}
		if (messageReceivedEvent.getMessage().getContentRaw().split(" ").length < 2)
			return null;
		return messageReceivedEvent.getMessage().getContentRaw().split(" ")[1];
	}

	public String getSubcommandGroup() {
		if (isSlashCommand()) {
			return slashCommandEvent.getSubcommandGroup();
		}
		return null;
	}

	boolean isSlashCommand() {
		return type == Type.SLASH_COMMAND;
	}

	public Data[] getArgs() {
		if (isSlashCommand()) {
			return Arrays.stream(slashCommandEvent.getOptions().toArray(OptionMapping[]::new)).map(
					optionMapping -> {
						CommandInfo.Option option = CommandInfo.from(this).getOption(optionMapping.getName());
						var data = switch (option.getType()) {
							case UNKNOWN -> null;
							case SUB_COMMAND -> null;
							case SUB_COMMAND_GROUP -> null;
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

		String command = messageReceivedEvent.getMessage().getContentRaw().startsWith(getJDA().getSelfUser().getAsMention())?
				messageReceivedEvent.getMessage().getContentRaw().substring(getJDA().getSelfUser().getAsMention().length() + 1) :
				messageReceivedEvent.getMessage().getContentRaw().substring(getPrefix().length());


		String argString = command.replaceFirst(getCommandName(), "");
		String[] args = argString.split(" ");
		args = handleQuotes(List.of(args));

		List<Data> result = new ArrayList<>();

		for (int i = 0; i < CommandInfo.from(this).args.length; i++) {
			if (i >= args.length)
				break;

			var arg = CommandInfo.from(this).args[i];
			if (i == CommandInfo.from(this).args.length - 1 && arg.getType() == OptionType.STRING) {
				result.add(new Data(arg, String.join(" ", Arrays.copyOfRange(args, i, args.length))));
				break;
			}
			result.add(new Data(arg, parse(args[i], arg.getType(), getChannel())));
		}


		return result.toArray(Data[]::new);
	}

	public record Data(CommandInfo.Option option, Object value) {
	}

	private <T> T parse(String arg, OptionType type, MessageChannel channel) {
		Object result = null;
		switch (type) {
			case ATTACHMENT -> {
				var attachments = messageReceivedEvent.getMessage().getAttachments();
				if (attachments.isEmpty()) {
					result = null;
					break;
				}
				result = attachments.getFirst();
			}
			case ROLE -> {
				if (channel instanceof GuildChannel) {
					try {
						result = ((GuildChannel) channel).getGuild().getRoleById(arg);
					} catch (NumberFormatException ignored) {
						var roleList = getGuild().getRolesByName(arg, true);
						result = roleList.isEmpty() ? null : roleList.getFirst();
					}
				}
			}
			case BOOLEAN -> result = Boolean.parseBoolean(arg);
			case CHANNEL -> {
				if (channel instanceof GuildChannel) {
					try {
						var res = ((GuildChannel) channel).getGuild().getGuildChannelById(arg);
						if (res != null) {
							result = res;
							break;
						}
						result = ((GuildChannel) channel).getGuild().getChannels().stream().filter(c ->
								c.getName().equalsIgnoreCase(arg)).toList().getFirst();
					} catch (Exception e) {
						result = null;
					}

				}
			}
			case INTEGER -> result = Integer.parseInt(arg);
			case MENTIONABLE -> {

			}
			case NUMBER -> result = Double.parseDouble(arg);
			case USER -> {
				List<Member> users = CacheManager.guildMemberCache.get(getGuild());

				users = users.stream().filter(user ->
						List.of(user.getId(), user.getEffectiveName(), user.getAsMention(),
										user.getUser().getName())
								.contains(arg)).toList();

				if (users.isEmpty())
					break;


				result = users.getFirst().getUser();
			}
			case SUB_COMMAND -> result = "SUB_COMMAND";
			case SUB_COMMAND_GROUP -> result = "SUB_COMMAND_GROUP";
			case STRING -> result = arg;
			case UNKNOWN -> result = null;
		}

		return (T) result;
	}

	private String[] handleQuotes(List<String> args) {
		LinkedList<String> args1 = new LinkedList<>(args);
		List<String> result = new ArrayList<>();

		Iterator<String> iterator = args1.iterator();
		while (iterator.hasNext()) {
			String arg = iterator.next();
			if (arg.startsWith("\"")) {
				StringBuilder quotedArg = new StringBuilder(arg.substring(1));
				while (!quotedArg.toString().endsWith("\"")) {
					if (!iterator.hasNext()) {
						// Handle the case where the closing quote is missing
						throw new IllegalArgumentException("Missing closing quote");
					}
					String nextArg = iterator.next();
					quotedArg.append(" ").append(nextArg);
				}

				result.add(quotedArg.substring(0, quotedArg.length()-1));
			} else {
				result.add(arg);
			}
		}

		result = result.stream().map(arg -> arg.replace("''", "\"")).toList();

		if (result.isEmpty() || result.getFirst().isEmpty()) {
			result = result.subList(1, result.size());
		}

		return result.toArray(String[]::new);
	}


	public Data getArg(int index) {
		return getArgs()[index];
	}


	public <T> T getArg(String name, Class<T> type) {
		CommandInfo from = CommandInfo.from(this);
		CommandInfo.Option option = from.getOption(name);

		if (option == null) {
			return null;
		}

		if (!this.isSlashCommand()) {
			for (Data arg :getArgs()) {
				if (arg.option.getName().equals(name)) {
					return type.cast(arg.value);
				}
			}

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
		if (isSlashCommand())
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
