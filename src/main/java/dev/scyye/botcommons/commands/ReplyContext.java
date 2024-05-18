package dev.scyye.botcommons.commands;

import dev.scyye.botcommons.menu.MenuManager;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ReplyContext {
	@Getter
	private String content;
	@Getter
	private boolean ephemeral = false;
	@Getter
	private final boolean defer = false;
	@Getter
	private final GenericCommandEvent.Type type;
	@Nullable
	private String menuId = null;
	@Nullable
	private final MessageReceivedEvent receivedEvent;
	@Nullable
	private final SlashCommandInteractionEvent interactionEvent;
	private final List<EmbedBuilder> embeds = new ArrayList<>();
	private final List<Object> menuArgs = new ArrayList<>();

	public ReplyContext(@NotNull MessageReceivedEvent event) {
		this.type = GenericCommandEvent.Type.MESSAGE_COMMAND;
		this.receivedEvent = event;
		this.interactionEvent = null;
	}

	public ReplyContext(@NotNull SlashCommandInteractionEvent event) {
		this.type = GenericCommandEvent.Type.SLASH_COMMAND;
		this.interactionEvent = event;
		this.receivedEvent = null;
	}

	public List<MessageEmbed> getEmbeds() {
		return embeds.stream().map(EmbedBuilder::build).toList();
	}

	public @Nullable String getMenuId() {
		return menuId;
	}

	public @Nullable MessageReceivedEvent getReceivedEvent() {
		return receivedEvent;
	}

	public @Nullable SlashCommandInteractionEvent getInteractionEvent() {
		return interactionEvent;
	}

	public ReplyContext content(String content) {
		this.content = content;
		return this;
	}

	public ReplyContext ephemeral() {
		this.ephemeral=true;
		return this;
	}

	public ReplyContext embed(EmbedBuilder embed) {
		this.embeds.add(embed);
		return this;
	}

	public ReplyContext menu(String menuId, Object... args) {
		this.menuArgs.addAll(List.of(args));
		this.menuId = menuId;
		return this;
	}

	public boolean reply() {
		return reply(ignored -> {});
	}

	public boolean reply(Consumer<Message> consumer) {
		if (receivedEvent != null) {
			if (menuId != null) {
				MenuManager.replyMenu(menuId, receivedEvent.getMessage(), menuArgs.toArray());
				this.menuId = null;
				this.embeds.clear();
				this.content = null;
				return true;
			}

			MessageCreateAction action = null;
			if (content == null && embeds.isEmpty()) {
				action = receivedEvent.getChannel().sendMessage("No content provided");
			} else if (content == null) {
				action = receivedEvent.getChannel().sendMessageEmbeds(getEmbeds());
			} else if (embeds.isEmpty()) {
				action = receivedEvent.getChannel().sendMessage(content);
			}

			if (action == null) {
				action = receivedEvent.getChannel().sendMessage("Error uwu");
			}

			action.queue(message -> {
				if (ephemeral)
					message.delete().delay(2, TimeUnit.SECONDS).queue();
				consumer.accept(message);
			});
			this.menuId = null;
			this.embeds.clear();
			this.content = null;
			return true;
		} else if (interactionEvent != null) {
			if (menuId != null) {
				if (!defer && !interactionEvent.isAcknowledged())
					interactionEvent.deferReply().queue();
				MenuManager.replyMenu(menuId, interactionEvent.getHook(), menuArgs.toArray());
				this.menuId = null;
				this.embeds.clear();
				this.content = null;
				return true;
			}
			if (defer) {
				interactionEvent.getHook().sendMessage(content).setEmbeds(getEmbeds()).setEphemeral(ephemeral).queue(consumer);
				this.menuId = null;
				this.embeds.clear();
				this.content = null;
				return true;
			}

			ReplyCallbackAction action = null;
			WebhookMessageCreateAction action2 = null;
			if (content == null && embeds.isEmpty()) {
				if (!interactionEvent.isAcknowledged())
					action = interactionEvent.reply("No content provided");
				else
					action2 = interactionEvent.getHook().sendMessage("No content provided");
			} else if (content == null) {
				if (!interactionEvent.isAcknowledged())
					action = interactionEvent.replyEmbeds(getEmbeds());
				else
					action2 = interactionEvent.getHook().sendMessageEmbeds(getEmbeds());
			} else if (embeds.isEmpty()) {
				if (!interactionEvent.isAcknowledged())
					action = interactionEvent.reply(content);
				else
					action2 = interactionEvent.getHook().sendMessage(content);
			}

			if (action!=null)
				action.setEphemeral(ephemeral).queue(hook -> {
					hook.retrieveOriginal().queue(consumer);
				});
			else if (action2!=null)
				action2.setEphemeral(ephemeral).queue(consumer);
			this.menuId = null;
			this.embeds.clear();
			this.content = null;
			return true;
		}
		this.menuId = null;
		this.embeds.clear();
		this.content = null;
		return false;
	}


}
