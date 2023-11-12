package dev.scyye.botcommons.utilities;

public class StringUtilities {
	public static String replaceSpecificCharacter(String string, char replacement, int index) {
		char[] chars = string.toCharArray();
		chars[index] = replacement;
		return new String(chars);
	}
}
