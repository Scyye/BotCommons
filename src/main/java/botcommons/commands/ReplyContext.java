package botcommons.commands;

import botcommons.menu.MenuManager;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ReplyContext {
	private volatile boolean finished = false;
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
	private OnceListener<? extends GenericEvent> once;

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

	public <T extends GenericEvent> ReplyContext listenOnce(Class<T> eventType, Predicate<T> filter, Function<T, Void> listener) {
		once = new OnceListener<>(eventType, interactionEvent.getJDA(), filter, listener);
		return this;
	}

	public boolean finish() {
		return finish(ignored -> {});
	}

	private void markAsFinished() {
		this.finished = true;
		this.menuId = null;
		this.embeds.clear();
		this.content = null;
		this.ephemeral = false;
		this.once = null;
	}

	public boolean finish(Consumer<Message> consumer) {
		if (finished)
			throw new IllegalStateException("ReplyContext already finished");
		if (once != null) {
			getInteractionEvent().getJDA().addEventListener(once);
		}
		if (menuId != null) {
			if (!defer && !interactionEvent.isAcknowledged())
				interactionEvent.deferReply().queue();
			MenuManager.replyMenu(menuId, interactionEvent.getHook(), menuArgs.toArray());
			if (menuId.endsWith("-fake")) {
				// in 5 minutes, delete the fake menu
				interactionEvent.getJDA().getGatewayPool().schedule(() -> {
					MenuManager.menuRegistry.remove(menuId);
				}, 5, TimeUnit.MINUTES);

			}
			markAsFinished();
			return true;
		}
		if (defer) {
			interactionEvent.getHook().sendMessage(content).setEmbeds(getEmbeds()).setEphemeral(ephemeral).queue(consumer);
			markAsFinished();
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
		markAsFinished();
		return true;
	}

	private static class OnceListener<T extends GenericEvent> extends ListenerAdapter {
		private final Function<T, Void> listener;
		private final JDA jda;
		private final Predicate<T> filter;
		private Duration timeout = Duration.ofMinutes(5);

		public OnceListener(Class<T> eventType, JDA jda, Predicate<T> filter, Function<T, Void> listener) {
			this.listener = listener;
			this.jda = jda;
			this.filter = filter;
			timeout();
		}

		public OnceListener<T> setTimeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		private ScheduledFuture<?> timeout() {
			long delay = timeout.getSeconds();
			if (delay <= 0) {
				delay = 1;
				System.out.println("Timeout duration must be positive, defaulting to 1 second");
			}
			return jda.getGatewayPool().schedule(() ->
					jda.removeEventListener(this),
					delay, TimeUnit.SECONDS);
		}

		@Override
		public void onGenericEvent(@NotNull GenericEvent event) {
			T typedEvent;
			try {
				typedEvent = (T) event;
				if (!filter.test(typedEvent))
					return;
			} catch (ClassCastException e) {
				return;
			}
			jda.removeEventListener(this);
			try {
				listener.apply(typedEvent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
