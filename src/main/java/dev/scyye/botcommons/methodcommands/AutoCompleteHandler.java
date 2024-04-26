package dev.scyye.botcommons.methodcommands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AutoCompleteHandler {
	String[] value();
}
