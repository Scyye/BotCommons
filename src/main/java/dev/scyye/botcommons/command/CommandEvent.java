package dev.scyye.botcommons.command;

import dev.scyye.botcommons.menu.PaginatedMenuHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Collection;

public class CommandEvent extends MessageReceivedEvent {
	public CommandEvent(JDA api, long responseNumber, Message message) {
		super(api, responseNumber, message);
	}

	public MessageCreateAction reply(String message) {
		return getMessage().reply(message);
	}

	public void replyMenu(MessageCreateData... data) {
		if (data.length == 1) {
			reply(data[0]).queue();
			return;
		}
		reply("LOADING MENU...\n\n(if this takes too long to update, contact this bot's creator)").queue(m -> {
			PaginatedMenuHandler.addMenu(PaginatedMenuHandler.buildMenu(m, data));
		});
	}

	public MessageCreateAction reply(StickerSnowflake... stickers) {
		return getMessage().replyStickers(stickers);
	}

	public MessageCreateAction reply(CharSequence content) {
		return getMessage().reply(content);
	}

	public MessageCreateAction reply(MessageCreateData msg) {
		return getMessage().reply(msg);
	}

	public MessageCreateAction reply(MessageEmbed embed, MessageEmbed... other) {
		return getMessage().replyEmbeds(embed, other);
	}

	public MessageCreateAction reply(LayoutComponent component, LayoutComponent... other) {
		return getMessage().replyComponents(component, other);
	}

	public MessageCreateAction reply(String format, Object... args) {
		return getMessage().replyFormat(format, args);
	}

	public MessageCreateAction reply(FileUpload... files) {
		return getMessage().replyFiles(files);
	}

	public MessageCreateAction reply(Collection<? extends FileUpload> files) {
		return getMessage().replyFiles(files);
	}

	public static CommandEvent from(MessageReceivedEvent event) {
		return new CommandEvent(event.getJDA(), event.getResponseNumber(), event.getMessage());
	}
}
