package dev.scyye.botcommons.menu.types;

import dev.scyye.botcommons.menu.IMenu;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public abstract class BaseMenu implements IMenu {
	private String messageId;

	@Override
	public abstract void handle(ButtonInteractionEvent event);

	@Override
	public abstract MessageEmbed build();

	@Override
	public String getMessageId() {
		return messageId;
	}

	@Override
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
}
