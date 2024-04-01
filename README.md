# Bot Commons
A framework for creating bots for Discord using [JDA](https://github.com/DV8FromTheWorld/JDA)


# Features
## Commands Framework
To create a command, simply create a class that implements `ICommand` and implement the `execute` method.
Make sure it is annotated with `@Command(name = "command name", help = "command help")`.
Then, register the command using `CommandManager#addCommands(ICommand...)`.

```java
import dev.scyye.botcommons.commands.Command;
import dev.scyye.botcommons.commands.CommandManager;
import dev.scyye.botcommons.commands.GenericCommandEvent;

@Command(name = "ping", help = "Replies with pong!", usage = "!ping")
public class PingCommand implements ICommand {
	@Override
	public void execute(GenericCommandEvent event) {
		event.reply("Pong!");
	}
}

public class Main {
	// ...
	public static void main(String[] args) {
		JDA jda = JDABuilder.createDefault("token")
				.addEventListeners(new CommandManager())
				.build();

		CommandManager.addCommands(new PingCommand());
	}
	// ...
}
```

## Config Framework
There are 2 types of configs: `Config` and `ServerConfig`.

`Config` is a config that is shared across all servers.\
`ServerConfig` is a config that is specific to a server.

### Config
To create a config, simply add this to your bot:
```java
public static void main(String[] args) {
    // ...
    Config.botName = "BotName"; // This will be used as the name of the config file
    Config config = Config.makeConfig(new HashMap<>(){{
        put("key", "value");
        put("another key", new String[]{"Values can be anything", "Such as lists"});
        put("a third key", new Player("PlayerName", 1000)); // Or even objects
    }});
    // ...
}
```

Then, to get a value, simply call `config#get(String key)`. I recommend storing the `Config` instance in a variable.

### GuildConfig
To create a server config, add this listener to your JDA instance, however you wish to do that.:
```java
public static void main(String[] args) {
    // ...
	JDA jda = JDABuilder.createDefault("token")
			.addEventListeners(new ConfigManager(new HashMap<>(){{
				put("key", "value");
				put("another key", new String[]{"Values can be anything", "Such as lists"});
				put("a third key", new Player("PlayerName", 1000)); // Or even objects
			}}))
			.build();
    // ...
}
```

### ***__NOTE:__ This will automatically create the `/sql` and `/config` commands*** 


# Disclaimer
EVERYTHING AFTER THIS POINT IS NOT ACCURATE. I WILL UPDATE IT SOON.


## Menu Framework
### ***__NOTE:__ YOU MUST call `JDA.addEventListener(PaginationListener)` to enable the menu framework.***

To create a menu, simply call `PaginatedMenuHandler#addMenu(PaginatedMenuHandler#buildMenu())`, and pass in the data for each page.

You can also reply to commands with a menu, by calling `CommandEvent#replyMenu(PaginatedMenuHandler#buildMenu())`.


### ***__NOTE:__ YOU MUST call `JDA.addEventListener(PaginationListener)` to enable the menu framework.***
