package botcommons.menu;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class MenuManager extends ListenerAdapter {
	private final JDA jda;
	public static final Map<String, IMenu> menuRegistry = new HashMap<>();

	public static MenuManager instance;

	/**
	 * Creates a new instance of {@link MenuManager}. This will register the instance with JDA and add itself as an event listener.
	 * @param jda The JDA instance to register with. This allows the manager to listen for button interactions.
	 */
	public MenuManager(JDA jda) {
		instance = this;
		this.jda = jda;
		jda.addEventListener(this);
	}

	/**
	 * Registers one or more menus to the menu registry. This allows the manager to handle button interactions for these menus.
	 * @param menus The menus to register. Each menu should be annotated with {@link botcommons.menu.Menu} to provide an ID.
	 */
	public static void registerMenu(IMenu... menus) {
		for (IMenu menu : menus) {
			String menuId = menu.getClass().getAnnotation(Menu.class).id();

			menuRegistry.put(menuId, menu);
		}
	}

	/**
	 * Registers a menu with a specific ID. This allows for dynamic registration of menus that may not be known at compile time.
	 * @param id The ID to register the menu with. This should be a unique identifier for the menu.
	 * @param menu The menu instance to register. This should implement the {@link IMenu} interface and provide the necessary functionality for handling button interactions.
	 */
	public static void registerMenuWithId(String id, IMenu menu) {
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("Menu ID cannot be null or empty");
		}
		if (menu == null) {
			throw new IllegalArgumentException("Menu cannot be null");
		}
		menuRegistry.put(id, menu);
	}

	/**
	 * Sends a menu to a specific text channel. This method builds the menu's embed and sends it as a message to the specified channel.
	 * @param menuId The ID of the menu to send
	 * @param channelId The channel to send it in
	 */
	public static void sendMenu(String menuId, String channelId) {
		IMenu menu = menuRegistry.get(menuId);

		if (menu==null)
			throw new IllegalArgumentException("Menu not found " + menuId);

		TextChannel channel = instance.jda.getTextChannelById(channelId);

		if (channel==null) {
			throw new IllegalArgumentException("Channel not found " + channelId);
		}

		channel.sendMessageEmbeds(menu.build()).addActionRow(menu.getButtons())
				.queue(message -> menu.setMessageId(message.getId()));
	}

	/**
	 * Sends a menu to a user's private channel. This method builds the menu's embed and sends it as a private message to the specified user.
	 * @param menuId The ID of the menu to send. This should correspond to a registered menu in the {@link MenuManager#menuRegistry}.
	 * @param userId The ID of the user to send the menu to. This should be a valid Discord user ID. The method will attempt to retrieve the user and send the private message.
	 */
	public static void sendMenuPrivate(String menuId, String userId) {
		IMenu menu = menuRegistry.get(menuId);

		if (menu == null)
			throw new IllegalArgumentException("Menu not found " + menuId);

		instance.jda.retrieveUserById(userId).queue(user ->
				user.openPrivateChannel().queue(channel ->
						channel.sendMessageEmbeds(menu.build()).addActionRow(menu.getButtons()).queue(message ->
								menu.setMessageId(message.getId()))));
	}

	private static IMenu getMenu(String menuId, Object... args) {
		IMenu menu = menuRegistry.get(menuId);
		try {
			if (menu.getClass().getConstructors().length>1&&args.length>0)
				menu = (IMenu) Arrays.stream(menu.getClass().getConstructors()).filter(constructor ->
						constructor.getParameterCount()!=0).toList().getFirst().newInstance(args);
			if (menuRegistry.containsKey(menuId))
				menuRegistry.replace(menuId, menu);
			else
				registerMenu(menu);
		} catch (Exception ignored) {}
		if (menu == null)
			menu = menuRegistry.get(menuId);
		if (menu == null)
			throw new IllegalArgumentException("Menu not found " + menuId);
		return menu;
	}

	@Deprecated
	public static void replyMenu(String menuId, Message message, Object... args) {
		IMenu menu = getMenu(menuId, args);

		splitButtons(message.replyEmbeds(menu.build()), menu.getButtons()).queue(message1 ->
				menu.setMessageId(message1.getId()));
	}

	/**
	 * Replies to a message with a menu. This method builds the menu's embed and sends it as a reply to the specified interaction hook.
	 * @param menuId The ID of the menu to send. This should correspond to a registered menu in the {@link MenuManager#menuRegistry}.
	 * @param hook The interaction hook to reply to. This is typically obtained from a slash command interaction or button interaction. It allows you to send a message in response to the interaction.
	 * @param args The arguments to pass into constructing the menu instance. This allows for dynamic creation of menus based on runtime parameters. The arguments should match the constructor of the menu class if it has one.
	 */
	public static void replyMenu(String menuId, InteractionHook hook, Object... args) {
		IMenu menu = getMenu(menuId, args);

		splitButtons(hook.sendMessageEmbeds(menu.build()), menu.getButtons()).queue(message1 ->
				menu.setMessageId(message1.getId()));
	}

	private static MessageCreateAction splitButtons(MessageCreateAction action, Button... buttons) {
		ActionRow.partitionOf(buttons).forEach(row -> action.addActionRow(row.getButtons()));
		return action;
	}

	private static WebhookMessageCreateAction<Message> splitButtons(WebhookMessageCreateAction<Message> action, Button... buttons) {
		ActionRow.partitionOf(buttons).forEach(row -> action.addActionRow(row.getButtons()));
		return action;
	}


	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String messageId = event.getMessageId();
		menuRegistry.values().stream()
				.filter(m -> m.getMessageId() != null && m.getMessageId().equals(messageId))
				.findFirst().ifPresent(menu -> menu.handle(event));
	}
}
