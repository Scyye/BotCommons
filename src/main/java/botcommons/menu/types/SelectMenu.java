package botcommons.menu.types;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public abstract class SelectMenu extends BaseMenu {
	static final Map<Integer, String> numberEmojis = new HashMap<>(){{
		put(1, "1️⃣");
		put(2, "2️⃣");
		put(3, "3️⃣");
		put(4, "4️⃣");
		put(5, "5️⃣");
		put(6, "6️⃣");
		put(7, "7️⃣");
		put(8, "8️⃣");
		put(9, "9️⃣");
		put(10, "🔟");
	}};


	@Override
	public void handle(ButtonInteractionEvent event) {
		// Do not acknowledge here unconditionally. Actions should acknowledge/reply if they need to.
		String[] parts = event.getComponentId().split("_");
		if (parts.length < 2) return;

		int index;
		try {
			index = Integer.parseInt(parts[1]);
		} catch (NumberFormatException e) {
			return;
		}

		Option[] options = getOptions();
		if (index < 0 || index >= options.length) return;

		options[index].action.accept(event);
	}

	@Override
	public MessageEmbed build() {
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Select Menu");

		int i = 1;

		for (Option option : getOptions()) {
			builder.addField(option.name + " " +numberEmojis.get(i), option.description, true);
			i++;
		}

		return builder.build();
	}

	@Override
	public Button[] getButtons() {
		Button[] buttons = new Button[getOptions().length];

		int i = 0;

		for (Option option : getOptions()) {
			buttons[i] = Button.secondary("select_" +i, numberEmojis.get(i + 1) + " " + option.name);
			i++;
		}

		return buttons;
	}

	protected abstract Option[] getOptions();

	protected record Option(String name, String description, Consumer<ButtonInteractionEvent> action) {
		public Option(String name, Consumer<ButtonInteractionEvent> action) {
			this(name, "", action);
		}

	}
}
