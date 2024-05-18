package dev.scyye.botcommons.cache;

import dev.scyye.botcommons.utilities.SQLiteUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CacheManager extends ListenerAdapter {
	public static HashMap<Guild, List<Member>> guildMemberCache = new HashMap<>();
	public static HashMap<User, List<Guild>> mutualGuildsCache = new HashMap<>();
	public static HashMap<MessageChannel, List<MessageStructure>> channelMessageCache = new HashMap<>();
	public static HashMap<Member, List<MessageStructure>> userMessageCache = new HashMap<>();
	public static HashMap<String, User> userCache = new HashMap<>();

	static void init(JDA jda) {
		jda.addEventListener(new CacheManager());
		SQLiteUtils.createCache(guildMemberCache, "guild_member_cache");
		SQLiteUtils.createCache(mutualGuildsCache, "mutual_guilds_cache");
		SQLiteUtils.createCache(channelMessageCache, "channel_message_cache");
		SQLiteUtils.createCache(userMessageCache, "user_message_cache");

		SQLiteUtils.createCache(userCache, "user_cache");
	}

	static void update() {
		SQLiteUtils.updateCache(guildMemberCache, "guild_member_cache");
		SQLiteUtils.updateCache(mutualGuildsCache, "mutual_guilds_cache");
		SQLiteUtils.updateCache(channelMessageCache, "channel_message_cache");
		SQLiteUtils.updateCache(userMessageCache, "user_message_cache");

		SQLiteUtils.updateCache(userCache, "user_cache");
	}

	// Message cache code
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		channelMessageCache.putIfAbsent(event.getChannel(), new ArrayList<>());
		channelMessageCache.get(event.getChannel()).add(MessageStructure.fromMessage(event.getMessage()));

		userMessageCache.putIfAbsent(event.getMember(), new ArrayList<>());
		userMessageCache.get(event.getMember()).add(MessageStructure.fromMessage(event.getMessage()));
	}

	@Override
	public void onMessageDelete(@NotNull MessageDeleteEvent event) {
		channelMessageCache.get(event.getChannel()).removeIf(messageStructure -> messageStructure.id().equals(event.getMessageId()));
		userMessageCache.forEach((member, messageStructures) -> messageStructures.removeIf(messageStructure -> messageStructure.id().equals(event.getMessageId())));
	}

	// Guild member cache, and mutual guilds cache code
	@Override
	public void onGuildReady(@NotNull GuildReadyEvent event) {
		event.getGuild().loadMembers().onSuccess(members -> {
			guildMemberCache.put(event.getGuild(), members);

			members.forEach(member -> {
				mutualGuildsCache.putIfAbsent(member.getUser(), new ArrayList<>());
				mutualGuildsCache.get(member.getUser()).add(event.getGuild());

				userCache.putIfAbsent(member.getUser().getId(), member.getUser());
			});
		});
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		guildMemberCache.get(event.getGuild()).add(event.getMember());
		mutualGuildsCache.putIfAbsent(event.getUser(), new ArrayList<>());
		mutualGuildsCache.get(event.getUser()).add(event.getGuild());
	}

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
		guildMemberCache.get(event.getGuild()).remove(event.getMember());
		mutualGuildsCache.get(event.getUser()).remove(event.getGuild());
	}

	public record MessageStructure(MessageChannel channel, MessageCreateData data, String id) {
		static MessageStructure fromMessage(Message message) {
			return new MessageStructure(message.getChannel(), MessageCreateData.fromMessage(message), message.getId());
		}
	}
}
