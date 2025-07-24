package botcommons.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/**
 * This annotation is used to define a command holder class.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandHolder {
	String group() default "N/A";
}
