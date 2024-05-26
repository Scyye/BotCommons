package dev.scyye.botcommons.methodcommands;

// usable on methods
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
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
