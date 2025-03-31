package botcommons.cache;

import botcommons.utilities.JsonUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * CacheManager is a utility class to manage various caches for JDA entities.
 */
@SuppressWarnings("unused")
public class CacheManager extends ListenerAdapter {
	private CacheManager() {}

	public static final HashMap<String, List<MemberStructure>> guildMemberCache = new HashMap<>();
	public static final HashMap<String, List<GuildStructure>> mutualGuildsCache = new HashMap<>();
	public static final HashMap<String, List<MessageStructure>> channelMessageCache = new HashMap<>();
	public static final HashMap<String, List<MessageStructure>> userMessageCache = new HashMap<>();
	public static final HashMap<String, User> userCache = new HashMap<>();

	/**
	 * Initializes the CacheManager with the provided JDA instance and cache options.
	 * @param jda The JDA instance to fetch data from. This cannot be null.
	 * @param guildMembers Whether to enable the guild member cache. This will load all members in each guild when they are ready.
	 * @param mutualGuilds Whether to enable the mutual guilds cache. This will store all guilds a user is in.
	 * @param channelMessages Whether to enable the channel messages cache. This will store all messages sent in guild channels.
	 * @param userMessages Whether to enable the user messages cache. This will store all messages sent by users in guild channels.
	 * @param users Whether to enable the user cache. This will store all users that are in guilds or have sent messages in guild channels.
	 */
	public static void init(@NotNull JDA jda, boolean guildMembers, boolean mutualGuilds, boolean channelMessages, boolean userMessages, boolean users) {
		if (!guildMembers && !mutualGuilds && !channelMessages && !userMessages && !users) throw new IllegalArgumentException("At least one cache must be enabled");
		jda.addEventListener(new CacheManager());



		if (guildMembers) JsonUtils.createCache(guildMemberCache, "guild_member_cache");
		if (mutualGuilds) JsonUtils.createCache(mutualGuildsCache, "mutual_guilds_cache");
		if (channelMessages) JsonUtils.createCache(channelMessageCache, "channel_message_cache");
		if (userMessages) JsonUtils.createCache(userMessageCache, "user_message_cache");
		if (users) JsonUtils.createCache(userCache, "user_cache");
	}

	/**
	 * Updates all caches to their respective JSON files.
	 */
	public static void update() {
		JsonUtils.updateCache(guildMemberCache, "guild_member_cache");
		JsonUtils.updateCache(mutualGuildsCache, "mutual_guilds_cache");
		JsonUtils.updateCache(channelMessageCache, "channel_message_cache");
		JsonUtils.updateCache(userMessageCache, "user_message_cache");

		JsonUtils.updateCache(userCache, "user_cache");
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (!event.isFromGuild())
			return;
		channelMessageCache.putIfAbsent(event.getChannel().getId(), new ArrayList<>());
		channelMessageCache.get(event.getChannel().getId()).add(MessageStructure.fromMessage(event.getMessage()));

		if (event.getMember() == null) return;

		userMessageCache.putIfAbsent(event.getAuthor().getId(), new ArrayList<>());
		userMessageCache.get(event.getAuthor().getId()).add(MessageStructure.fromMessage(event.getMessage()));
		update();
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event) {
		if (!event.isFromGuild())
			return; // Ignore DMs
		channelMessageCache.get(event.getChannel().getId()).removeIf(messageStructure -> messageStructure.id().equals(event.getMessageId()));
		userMessageCache.forEach((member, messageStructures) -> messageStructures.removeIf(messageStructure -> messageStructure.id().equals(event.getMessageId())));
		update();
	}

	// Guild member cache, and mutual guilds cache code
	@Override
	public void onGuildReady(@NotNull GuildReadyEvent event) {
		event.getGuild().loadMembers().onSuccess(members -> {
			System.out.printf("Loaded %d members for guild %s%n", members.size(), event.getGuild().getName());
			guildMemberCache.put(event.getGuild().getId(), members.stream().map(MemberStructure::fromMember).toList());

			members.forEach(member -> {
				mutualGuildsCache.putIfAbsent(member.getUser().getId(), new ArrayList<>());
				mutualGuildsCache.get(member.getUser().getId()).add(GuildStructure.fromGuild(event.getGuild()));

				userCache.putIfAbsent(member.getUser().getId(), member.getUser());
			});
			update();
		}).onError(Throwable::printStackTrace);
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		guildMemberCache.get(event.getGuild().getId()).add(MemberStructure.fromMember(event.getMember()));
		mutualGuildsCache.putIfAbsent(event.getUser().getId(), new ArrayList<>());
		mutualGuildsCache.get(event.getUser().getId()).add(GuildStructure.fromGuild(event.getGuild()));
		update();
	}

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
		guildMemberCache.get(event.getGuild().getId()).removeIf(memberStructure -> memberStructure.id().equals(event.getUser().getId()));
		mutualGuildsCache.get(event.getUser().getId()).remove(GuildStructure.fromGuild(event.getGuild()));
		update();
	}

	/**
	 * Represents a message structure that holds information about a message in a guild.
	 * @param id The unique identifier of the message.
	 * @param content The raw content of the message.
	 * @param contentDisplay The content of the message as it would be displayed, which may differ from the raw content due to formatting or mentions.
	 * @param channel The channel structure where the message was sent. This provides context about the channel in which the message resides.
	 * @param author The author of the message, represented as a MemberStructure. This can be null if the message was sent by a bot or if the author information is not available.
	 */
	public record MessageStructure(String id, String content, String contentDisplay, ChannelStructure channel, @Nullable MemberStructure author) {
		static MessageStructure fromMessage(Message message) {
			return new MessageStructure(message.getId(), message.getContentRaw(), message.getContentDisplay(), ChannelStructure.fromChannel(message.getGuildChannel()), MemberStructure.fromMember(message.getMember()));
		}
	}

	/**
	 * Represents a structure for a guild, containing information about its channels, member count, name, and ID.
	 * @param channels A list of ChannelStructure objects representing the channels in the guild. This provides an overview of the communication channels available within the guild.
	 * @param memberCount The total number of members in the guild. This gives an indication of the size of the guild and its community.
	 * @param name The name of the guild. This is a human-readable identifier for the guild, making it easier to recognize and refer to.
	 * @param id The unique identifier of the guild. This is a string that serves as a primary key for identifying the guild within the Discord API and is essential for performing operations related to this specific guild.
	 */
	public record GuildStructure(List<ChannelStructure> channels, int memberCount, String name, String id) {
		static GuildStructure fromGuild(Guild guild) {
			return new GuildStructure(guild.getChannels().stream().map(ChannelStructure::fromChannel).toList(), guild.getMemberCount(), guild.getName(), guild.getId());
		}
	}

	/**
	 * Represents a structure for a channel within a guild, encapsulating its unique identifier, the guild it belongs to, and its name.
	 * @param id The unique identifier of the channel. This serves as a primary key for identifying the channel within the Discord API, allowing for precise operations related to this specific channel.
	 * @param guild The unique identifier of the guild to which this channel belongs. This provides context about the guild structure and allows for operations that are specific to the guild.
	 * @param name The name of the channel. This is a human-readable identifier for the channel, making it easier to recognize and refer to. It can be used in user interfaces or logs to display the channel's name instead of its ID.
	 */
	public record ChannelStructure(String id, String guild, String name) {
		static ChannelStructure fromChannel(GuildChannel channel) {
			return new ChannelStructure(channel.getId(), channel.getGuild().getId(), channel.getName());
		}
	}

	/**
	 * Represents a structure for a member within a guild, encapsulating their unique identifier, the guild they belong to, and their name.
	 * @param id The unique identifier of the member. This serves as a primary key for identifying the member within the Discord API, allowing for precise operations related to this specific member.
	 * @param guild The {@link GuildStructure} representing the guild to which this member belongs. This provides context about the guild structure and allows for operations that are specific to the guild.
	 * @param name The name of the member as it would be displayed in Discord. This is a human-readable identifier for the member, making it easier to recognize and refer to them. It can be used in user interfaces or logs to display the member's name instead of their ID.
	 */
	public record MemberStructure(String id, GuildStructure guild, String name) {
		static MemberStructure fromMember(Member member) {
			return new MemberStructure(member.getId(), GuildStructure.fromGuild(member.getGuild()), member.getEffectiveName());
		}
	}
}
