package dev.scyye.botcommons.menu;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static dev.scyye.botcommons.menu.PaginatedMenuHandler.*;

public class PaginationListener extends ListenerAdapter {

	JDA jda;

	public PaginationListener(JDA jda) {
		this.jda = jda;
	}

	@Override
	public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
		if (event.getUser().isBot()) return;

		if (event.getMessageAuthorId().equals(jda.getSelfUser().getId())) {
			if (event.getUserId().equals("553652308295155723"))
				if (event.getEmoji().asUnicode().getName().equals("❌"))
					event.getChannel().deleteMessageById(event.getMessageId()).queue();
		}


		event.getChannel().getHistory().retrievePast(25).queue(messages -> {
			messages.forEach(message -> {
				if (message.getId().equals(event.getMessageId())) {
					if (!currentMenus.stream().map(menu -> menu.message).toList().contains(message)) return;
					if (event.getEmoji().asUnicode().getName().equals("➡️")) {
						incrementPage(message);
						event.getReaction().removeReaction(event.getUser()).queue();
						updateMenus();
					} else if (event.getEmoji().asUnicode().getName().equals("⬅️")) {
						decrementPage(message);
						event.getReaction().removeReaction(event.getUser()).queue();
						updateMenus();
					}
				}
			});
		});
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (event.getMessage().getReferencedMessage()==null)return;
		if (!event.getMessage().getReferencedMessage().getAuthor().getId().equals(jda.getSelfUser().getId()))return;
		if (event.getMessage().getAuthor().isBot()) return;
		if (!currentMenus.stream().map(menu -> menu.message).toList().contains(event.getMessage().getReferencedMessage())) return;

		Message msg = event.getMessage().getReferencedMessage();

		if (event.getMessage().getContentRaw().equalsIgnoreCase(">")) {
			System.out.println("Incrementing page");
			incrementPage(msg);
			updateMenus();
			event.getMessage().delete().queue();
		}
		if (event.getMessage().getContentRaw().equalsIgnoreCase("<")) {
			System.out.println("Decrementing page");
			decrementPage(msg);
			updateMenus();
			event.getMessage().delete().queue();
		}
	}
}
