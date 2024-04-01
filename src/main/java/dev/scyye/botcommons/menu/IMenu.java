package dev.scyye.botcommons.menu;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public interface IMenu {
	void handle(ButtonInteractionEvent event);
	MessageEmbed build();
	String getMessageId();
	void setMessageId(String messageId);
	default Button[] getButtons() {
		return new Button[0];
	}
}
