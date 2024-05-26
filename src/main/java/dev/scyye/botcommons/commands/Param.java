package dev.scyye.botcommons.commands;

import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Param {
	String description();
	boolean required() default true;
	// TODO: Add support for choices
	String[] choices() default {};
	boolean autocomplete() default false;
	OptionType type() default OptionType.STRING;
}
