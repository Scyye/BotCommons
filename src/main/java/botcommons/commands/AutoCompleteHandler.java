package botcommons.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCompleteHandler {
	/**
	 * The name of the command that this auto complete handler is for.
	 * @return value
	 */
	String[] value();
}
