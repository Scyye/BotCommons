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

	private MenuManager(JDA jda) {
		System.out.println("Initializing MenuManager");
		instance = this;
		this.jda = jda;
		jda.addEventListener(this);
	}

	public static void init(JDA jda) {
		if (instance == null) {
			new MenuManager(jda);
		} else {
			throw new IllegalStateException("MenuManager already initialized.");
		}
	}

	public static void registerMenu(IMenu... menus) {
		if (instance == null) {
			throw new IllegalStateException("MenuManager not initialized. Please initialize first.");
		}
		for (IMenu menu : menus) {
			System.out.println("Registering menu: " + menu.getClass().getName());
			String menuId = menu.getClass().getAnnotation(Menu.class).id();

			menuRegistry.put(menuId, menu);
		}
	}

	public static void registerMenuWithId(String id, IMenu menu) {
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("Menu ID cannot be null or empty");
		}
		if (menu == null) {
			throw new IllegalArgumentException("Menu cannot be null");
		}
		menuRegistry.put(id, menu);
	}

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
		System.out.println("Button interaction received");
		String messageId = event.getMessageId();
		menuRegistry.values().stream()
				.filter(m -> m.getMessageId() != null && m.getMessageId().equals(messageId))
				.findFirst().ifPresent(menu -> menu.handle(event));
	}
}
