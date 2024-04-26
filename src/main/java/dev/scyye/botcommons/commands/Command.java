package dev.scyye.botcommons.commands;

import net.dv8tion.jda.annotations.ForRemoval;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use the new {@link dev.scyye.botcommons.methodcommands.MethodCommandManager} and other method command classes instead
 */
@Deprecated
@ForRemoval
// usable on classes
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Command {
	String name();
	String help();
	String[] aliases() default {};
	String usage() default "";
	String category() default "GENERAL";
	String permission() default "MESSAGE_SEND";
	Scope scope() default Scope.BOTH;

	enum Scope {
		GUILD,
		DM,
		BOTH
	}
}
