package dev.scyye.botcommons.command;

public interface TextCommand extends Command {
	String getHelp();

	/**
	 * Returns the usage of the command.
	 * @example	!command <required arg> [optional arg]
	 */
	String getUsage();
	void execute(CommandEvent event, String[] args);

	default String[] getAliases() {
		return new String[0];
	}
}
