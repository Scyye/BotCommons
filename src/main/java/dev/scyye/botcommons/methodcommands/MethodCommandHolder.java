package dev.scyye.botcommons.methodcommands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MethodCommandHolder {
	String group() default "N/A";
}
