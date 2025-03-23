package botcommons.cache;

import botcommons.utilities.JsonUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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

@SuppressWarnings("unused")
public class CacheManager extends ListenerAdapter {
	private CacheManager() {}

	public static final HashMap<String, List<MemberStructure>> guildMemberCache = new HashMap<>();
	public static final HashMap<String, List<GuildStructure>> mutualGuildsCache = new HashMap<>();
	public static final HashMap<String, List<MessageStructure>> channelMessageCache = new HashMap<>();
	public static final HashMap<String, List<MessageStructure>> userMessageCache = new HashMap<>();
	public static final HashMap<String, User> userCache = new HashMap<>();

	public static void init(JDA jda, boolean guildMembers, boolean mutualGuilds, boolean channelMessages, boolean userMessages, boolean users) {
		if (jda == null) throw new IllegalArgumentException("JDA instance cannot be null");
		if (!guildMembers && !mutualGuilds && !channelMessages && !userMessages && !users) throw new IllegalArgumentException("At least one cache must be enabled");
		jda.addEventListener(new CacheManager());



		if (guildMembers) JsonUtils.createCache(guildMemberCache, "guild_member_cache");
		if (mutualGuilds) JsonUtils.createCache(mutualGuildsCache, "mutual_guilds_cache");
		if (channelMessages) JsonUtils.createCache(channelMessageCache, "channel_message_cache");
		if (userMessages) JsonUtils.createCache(userMessageCache, "user_message_cache");
		if (users) JsonUtils.createCache(userCache, "user_cache");
	}

	public static void update() {
		JsonUtils.updateCache(guildMemberCache, "guild_member_cache");
		JsonUtils.updateCache(mutualGuildsCache, "mutual_guilds_cache");
		JsonUtils.updateCache(channelMessageCache, "channel_message_cache");
		JsonUtils.updateCache(userMessageCache, "user_message_cache");

		JsonUtils.updateCache(userCache, "user_cache");
	}

	// Message cache code
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		channelMessageCache.putIfAbsent(event.getChannel().getId(), new ArrayList<>());
		channelMessageCache.get(event.getChannel().getId()).add(MessageStructure.fromMessage(event.getMessage()));

		if (event.getMember() == null) return;

		userMessageCache.putIfAbsent(event.getAuthor().getId(), new ArrayList<>());
		userMessageCache.get(event.getAuthor().getId()).add(MessageStructure.fromMessage(event.getMessage()));
		update();
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event) {
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

	public record MessageStructure(String id, String content, String contentDisplay, ChannelStructure channel, @Nullable MemberStructure author) {
		static MessageStructure fromMessage(Message message) {
			return new MessageStructure(message.getId(), message.getContentRaw(), message.getContentDisplay(), ChannelStructure.fromChannel(message.getGuildChannel()), MemberStructure.fromMember(message.getMember()));
		}
	}

	public record GuildStructure(List<ChannelStructure> channels, int memberCount, String name, String id) {
		static GuildStructure fromGuild(Guild guild) {
			return new GuildStructure(guild.getChannels().stream().map(ChannelStructure::fromChannel).toList(), guild.getMemberCount(), guild.getName(), guild.getId());
		}
	}

	public record ChannelStructure(String id, String guild, String name) {
		static ChannelStructure fromChannel(GuildChannel channel) {
			return new ChannelStructure(channel.getId(), channel.getGuild().getId(), channel.getName());
		}
	}

	public record MemberStructure(String id, GuildStructure guild, String name) {
		static MemberStructure fromMember(Member member) {
			return new MemberStructure(member.getId(), GuildStructure.fromGuild(member.getGuild()), member.getEffectiveName());
		}
	}
}
