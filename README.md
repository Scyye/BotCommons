# Bot Commons
A framework for creating bots for Discord using [JDA](https://github.com/DV8FromTheWorld/JDA)


# Features
## Commands Framework
To get started, call `CommandManager#init()` when starting your bot. 
There are 2 parameters, both booleans to enable useful commands. `!help` and `!tree`. Help, displays a list of commands,
and tree displays a tree for group & sub commands.

To create a command, simply create a class that implements `TextCommand` and implement the `execute` method.
Then, register the command using `CommandManager#addCommand(Command)`.

```java
public class PingCommand implements TextCommand {
    @Override
    public String getName() {
        return "ping";
    }
    public String getDescription() {
        return "Replies with pong!";
    }
	
	public String getUsage() {
        return "!ping";
    }
	
    @Override
    public void execute(CommandEvent event, String[] args) {
        event.reply("Pong!");
    }
}

public class Main {
    // ...
    public static void main(String[] args) {
        CommandManager.init(true, true);
        CommandManager.addCommand(new PingCommand());
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
    Config config = Config.makeConfig(new HashMap<>(){{
        put("key", "value");
        put("another key", new String[]{"Values can be anything", "Such as lists"})
        put("a third key", new Player("PlayerName", 1000)); // Or even objects
    }}, "Bot Name (for file name)");
    // ...
}
```

Then, to get a value, simply call `config#get(String key)`. I recommend storing the `Config` instance in a variable.

### ServerConfig
To create a server config, add this to a `GuildReadyEvent` listener:
```java
public void onGuildReady(GuildReadyEvent event) {
    // ...
        ServerConfig serverConfig = ServerConfig.createConfig(event.getGuild().getId(), new HashMap<>() {{
            put("key", "value");
            put("another key", new String[]{"Values can be anything", "Such as lists"})
            put("a third key", new Player("PlayerName", 1000)); // Or even objects
        }});
    // ...
}
```

## Menu Framework
### ***__NOTE:__ YOU MUST call `JDA.addEventListener(PaginationListener)` to enable the menu framework.***

To create a menu, simply call `PaginatedMenuHandler#addMenu(PaginatedMenuHandler#buildMenu())`, and pass in the data for each page.

You can also reply to commands with a menu, by calling `CommandEvent#replyMenu(PaginatedMenuHandler#buildMenu())`.

### ***__NOTE:__ YOU MUST call `JDA.addEventListener(PaginationListener)` to enable the menu framework.***
