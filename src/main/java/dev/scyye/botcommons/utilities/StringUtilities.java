package dev.scyye.botcommons.utilities;

@Deprecated(since = "1.7", forRemoval = true)
public class StringUtilities {
	public static String replaceSpecificCharacter(String string, char replacement, int index) {
		char[] chars = string.toCharArray();
		chars[index] = replacement;
		return new String(chars);
	}
}
