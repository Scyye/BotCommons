# BotCommons

A powerful framework for creating Discord bots using [JDA (Java Discord API)](https://github.com/DV8FromTheWorld/JDA) that simplifies common bot development tasks.

![Version](https://img.shields.io/badge/version-1.11-blue)
![Java](https://img.shields.io/badge/java-21-orange)
![License](https://img.shields.io/badge/license-MIT-green)

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Commands Framework](#commands-framework)
- [Config Framework](#config-framework)
- [Menu Framework](#menu-framework)
- [Cache Framework](#cache-framework)

## Features

BotCommons provides several key features to simplify Discord bot development:

- **Commands Framework**: Easy creation and management of slash commands with parameter validation
- **Config Framework**: Simple configuration management for global and per-guild settings
- **Menu Framework**: Interactive menu system for user interactions
- **Cache Framework**: Efficient caching of Discord entities like messages, guilds, channels and members

## Installation

### Maven

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.scyye</groupId>
    <artifactId>BotCommons</artifactId>
    <version>1.11</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```groovy
dependencies {
    implementation 'dev.scyye:BotCommons:1.11'
}
```

## Commands Framework

The Commands Framework simplifies the creation and management of slash commands.

### Creating Commands

1. Create a class annotated with `@CommandHolder`
2. Create methods annotated with `@Command`
3. Register the commands with the `CommandManager`

```java
import botcommons.commands.*;

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
```

### Setting Up CommandManager

```java
public class Main {
    public static void main(String[] args) {
        JDA jda = JDABuilder.createDefault("token")
                .addEventListeners(new CommandManager())
                .build();
        
        // Initialize the CommandManager
        CommandManager.init(jda);
        
        // Register commands
        CommandManager.addCommands(PingCommand.class);
    }
}
```

### Parameter Annotations

Use the `@Param` annotation to define command parameters:

```java
@Param(
    description = "Description of the parameter",
    required = true, // Default is true
    type = Param.ParamType.USER, // Type of parameter (STRING, INTEGER, USER, etc.)
    autocomplete = true // Enable autocomplete (requires implementing AutoCompleteHandler)
)
```

## Config Framework

The Config Framework provides both global and per-guild configuration management.

### Global Config

```java
public static void main(String[] args) {
    // Set the bot name for the config file
    Config.botName = "MyBot";
    
    // Create a default config
    Config config = Config.makeConfig(new HashMap<>() {{
        put("prefix", "!");
        put("admins", new String[]{"userId1", "userId2"});
        put("defaultSettings", new HashMap<String, Object>() {{
            put("welcomeMessage", true);
            put("loggingEnabled", false);
        }});
    }});
    
    // Access config values
    String prefix = config.get("prefix", String.class);
    boolean loggingEnabled = ((Map<String, Object>)config.get("defaultSettings")).get("loggingEnabled");
}
```

### Guild Config

```java
public static void main(String[] args) {
    JDA jda = JDABuilder.createDefault("token")
            .addEventListeners(new ConfigManager(new HashMap<>() {{
                put("prefix", "!");
                put("welcomeMessage", true);
                put("loggingEnabled", false);
            }}))
            .build();
            
    // Later, get a guild's config
    Config guildConfig = event.getConfig();
    boolean welcomeMessage = guildConfig.get("welcomeMessage", Boolean.class);
    
    // Update a value
    ConfigManager.getInstance().setValue(guildId, "welcomeMessage", false);
}
```

**Note:** This will automatically create the `/sql` and `/config` commands.

## Menu Framework

The Menu Framework allows you to create interactive menus with buttons and pagination.

### Setting Up MenuManager

```java
public static void main(String[] args) {
    JDA jda = JDABuilder.createDefault("token")
            .build();
            
    // Create menu manager instance
    MenuManager menuManager = new MenuManager(jda);
}
```

### Creating and Registering Menus

```java
// Create a simple menu implementation
IMenu helpMenu = new IMenu() {
    @Override
    public String getMenuId() {
        return "help_menu";
    }
    
    @Override
    public MessageCreateAction generateMenu(Object... args) {
        return new MessageCreateAction()
            .setContent("Help Menu")
            .addActionRow(
                Button.primary("prev", "Previous"),
                Button.primary("next", "Next")
            );
    }
    
    @Override
    public void handleButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (buttonId.equals("prev")) {
            // Handle previous button
        } else if (buttonId.equals("next")) {
            // Handle next button
        }
    }
};

// Register the menu
menuManager.registerMenu(helpMenu);

// Send the menu to a channel
menuManager.sendMenu("help_menu", channelId);

// Or reply to a command with a menu
event.replyMenu("help_menu").finish();
```

## Cache Framework

The Cache Framework allows efficient caching of Discord entities for improved performance.

### Initializing Cache

```java
public static void main(String[] args) {
    JDA jda = JDABuilder.createDefault("token")
            .build();
    
    // Initialize cache with various options
    CacheManager.init(
        jda,
        true,  // Cache guild members
        true,  // Cache mutual guilds
        true,  // Cache channel messages
        true,  // Cache user messages
        true   // Cache users
    );
}
```

### Accessing Cached Data

```java
// Access cached members for a guild
List<CacheManager.MemberStructure> members = CacheManager.guildMemberCache.get(guildId);

// Access cached messages
List<CacheManager.MessageStructure> messages = CacheManager.channelMessageCache.get(channelId);

// Update the cache to JSON files
CacheManager.update();
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
