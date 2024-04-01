package dev.scyye.botcommons.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// usable on classes
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Command {
	String name();
	String help();
	String[] aliases() default {};
	String usage() default "";
	Category category() default Category.OTHER;
	String permission() default "MESSAGE_SEND";
	Scope scope() default Scope.BOTH;

	enum Scope {
		GUILD,
		TICKET,
		DM,
		BOTH
	}
	enum Category {
		TICKET,
		UTILITY,
		FUN,
		OTHER,
		MODERATION,
		ADMINISTRATION,
		YOU_CANT_USE_THIS_LOL_LOSER_GET_OWNED,
	}
}
