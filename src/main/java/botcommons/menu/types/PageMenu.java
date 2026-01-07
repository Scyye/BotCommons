package botcommons.menu.types;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.List;

public abstract class PageMenu extends BaseMenu {
	private int currentPage = 0;

	@Override
	public void handle(ButtonInteractionEvent event) {
		String compId = event.getComponentId();

		List<MessageEmbed> pages = getPages();

		// If there's only one page, replace buttons with a single "end" button and return
		if (pages.size() == 1 && !compId.equalsIgnoreCase("end")) {
			event.editMessageEmbeds(pages.get(0))
					.setActionRow(Button.of(ButtonStyle.DANGER, "end", Emoji.fromUnicode("❌")))
					.queue();
			return;
		}

		switch (compId) {
			case "left" -> {
				if (currentPage > 0) {
					currentPage--;
				} else {
					currentPage = pages.size() - 1;
				}
			}
			case "right" -> {
				if (currentPage < pages.size() - 1) {
					currentPage++;
				} else {
					currentPage = 0;
				}
			}
			case "end" -> {
				event.getMessage().delete().queue();
				return;
			}
		}

		// single edit/ack for the interaction
		event.editMessageEmbeds(pages.get(currentPage)).queue();
	}

	public abstract List<EmbedBuilder> getPageData();

	public final List<MessageEmbed> getPages() {
		for (int i = 0; i < getPageData().size(); i++) {
			EmbedBuilder embedBuilder = getPageData().get(i);
			embedBuilder.setFooter("Page " + (i + 1) + "/" + getPageData().size());
		}

		return getPageData().stream().map(EmbedBuilder::build).toList();
	}

	@Override
	public MessageEmbed build() {
		return getPages().get(0);
	}

	@Override
	public Button[] getButtons() {
		return new Button[]{
				Button.of(ButtonStyle.PRIMARY, "left", Emoji.fromUnicode("⬅️")),
				Button.of(ButtonStyle.SECONDARY, "right", Emoji.fromUnicode("➡️")),
		};
	}
}
