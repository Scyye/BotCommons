package botcommons.commands;

import botcommons.menu.MenuManager;
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
	@Nullable
	private String menuId = null;
	@NotNull
	private final SlashCommandInteractionEvent interactionEvent;
	private final List<EmbedBuilder> embeds = new ArrayList<>();
	private final List<Object> menuArgs = new ArrayList<>();

	public ReplyContext(@NotNull SlashCommandInteractionEvent event) {
		this.interactionEvent = event;
	}

	public List<MessageEmbed> getEmbeds() {
		return embeds.stream().map(EmbedBuilder::build).toList();
	}

	public @Nullable String getMenuId() {
		return menuId;
	}

	public @NotNull SlashCommandInteractionEvent getInteractionEvent() {
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

	public boolean finish() {
		return finish(ignored -> {});
	}

	public boolean finish(Consumer<Message> consumer) {
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
		WebhookMessageCreateAction<Message> action2 = null;
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
			action.setEphemeral(ephemeral).queue(hook ->
					hook.retrieveOriginal().queue(consumer)
			);
		else if (action2!=null)
			action2.setEphemeral(ephemeral).queue(consumer);
		this.menuId = null;
		this.embeds.clear();
		this.content = null;
		return true;
	}


}
