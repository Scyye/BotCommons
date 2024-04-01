package dev.scyye.botcommons.menu.impl;

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
		if (event.getComponentId().equals("left")) {
			if (currentPage > 0) {
				currentPage--;
			} else {
				currentPage = getPages().size() - 1;
			}
		} else if (event.getComponentId().equals("right")) {
			if (currentPage < getPages().size() - 1) {
				currentPage++;
			} else {
				currentPage = 0;
			}
		}
		event.editMessageEmbeds(getPages().get(currentPage)).queue();
	}

	public abstract List<EmbedBuilder> getPageData();

	public final List<MessageEmbed> getPages() {
		for (int i = 0; i < getPageData().size(); i++) {
			EmbedBuilder embedBuilder = getPageData().get(i);
			embedBuilder.setFooter(STR."Page \{i + 1}/\{getPageData().size()}");
		}

		return getPageData().stream().map(EmbedBuilder::build).toList();
	}

	@Override
	public MessageEmbed build() {
		return getPages().getFirst();
	}

	@Override
	public Button[] getButtons() {
		return new Button[]{
				Button.of(ButtonStyle.PRIMARY, "left", Emoji.fromUnicode("⬅️")),
				Button.of(ButtonStyle.SECONDARY, "right", Emoji.fromUnicode("➡️"))
		};
	}
}
