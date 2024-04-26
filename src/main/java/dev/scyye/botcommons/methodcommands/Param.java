package dev.scyye.botcommons.methodcommands;

import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Param {
	String name();
	String description();
	boolean required() default true;
	String[] choices() default {};
	boolean autocomplete() default false;
	OptionType type() default OptionType.STRING;
}
