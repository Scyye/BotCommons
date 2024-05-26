# Bot Commons
A framework for creating bots for Discord using [JDA](https://github.com/DV8FromTheWorld/JDA)


# Features
## Commands Framework
To create a command, simply create a class that is annotated with `@MethodCommandHolder(string: Optional group)`;
Then create a function that is annotated with `@MethodCommand()`. (View the `@MethodCommand` class for more information on the parameters.)
Then, register the command using `MethodCommandManager.addCommands(CommandHolderClass.class)`.

**Below is an example of a command, along with how to use parameters:**

```java
import dev.scyye.botcommons.commands.*;
import dev.scyye.botcommons.commands.CommandManager;

// You can also specify a group for the commands in the holder
@CommandHolder
public class PingCommand {
	@Command(name = "ping", help = "Pong!")
	public void execute(GenericCommandEvent event,
						@Param(
								description = "A user",
								type = Param.ParamType.USER
						)
						// the name of the argument is grabbed from the parameter name        
						User user) {
		event.replySuccess("Pong! " + user.getAsMention()).finish(message -> {
			// Success consumer
		});
	}
}

public class Main {
	// ...
	public static void main(String[] args) {
		JDA jda = JDABuilder.createDefault("token")
				.addEventListeners(new CommandManager())
				.build();

		CommandManager.addCommands(PingCommand.class);
	}
	// ...
}
```

## Config Framework
There are 2 types of configs: `Config` and `GuildConfig`.

`Config` is a config that is shared across all servers.\
`GuildConfig` is a config that is specific to a server.

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

## Cache Framework
### TODO
