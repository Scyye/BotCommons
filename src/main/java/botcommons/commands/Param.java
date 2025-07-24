package botcommons.commands;

import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to define a parameter for a command method.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {
	/**
	 * The name of the parameter.
	 */
	String description();

	/**
	 * Whether this parameter is required or optional.
	 */
	boolean required() default true;

	/**
	 * An array of choices for the parameter. This is used to provide a list of valid options for the parameter in the command interface.
	 */
	String[] choices() default {};

	/**
	 * Whether to enable autocomplete for this parameter. If set to true, the bot will provide suggestions for this parameter based on user input.
	 */
	boolean autocomplete() default false;

	/**
	 * The {@link OptionType} of the parameter. This defines the data type of the parameter in the command interface (e.g. STRING, INTEGER, BOOLEAN, etc.).
	 * Default is STRING.
	 */
	OptionType type() default OptionType.STRING;
}
