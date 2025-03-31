package botcommons.commands;

// usable on methods
import net.dv8tion.jda.api.interactions.InteractionContextType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to define a command for the bot.
 * <p>
 * The name, help, aliases, usage, category and permission can be specified.
 * <p>
 * The userContext defines where this command can be used (e.g. GUILD, PRIVATE_CHANNEL)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Command {
	String name();
	String help();
	String[] aliases() default {};
	String usage() default "";
	String category() default "GENERAL";
	String permission() default "MESSAGE_SEND";
	InteractionContextType[] userContext() default {InteractionContextType.GUILD, InteractionContextType.BOT_DM};
}
