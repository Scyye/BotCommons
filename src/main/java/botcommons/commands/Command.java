package botcommons.commands;

// usable on methods
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.dv8tion.jda.api.interactions.commands.Command.Type;

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
	Type type() default Type.SLASH;
	IntegrationType[] integrationTypes() default {IntegrationType.GUILD_INSTALL};
}
