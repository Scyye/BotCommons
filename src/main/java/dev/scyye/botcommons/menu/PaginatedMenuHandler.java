package dev.scyye.botcommons.menu;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.SplitUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.*;

public class PaginatedMenuHandler {
	static List<Menu> currentMenus = new ArrayList<>();

	public static void updateMenus() {
		for (var menu : currentMenus) {
			var page = menu.pages.get(menu.page);
			menu.message.editMessage(MessageEditData.fromCreateData(page.content)).queue();
		}
	}

	public static void addMenu(Menu menu) {
		currentMenus.add(menu);
		var msg = menu.message;
		msg.editMessage(MessageEditData.fromCreateData(menu.pages.get(menu.page).content)).queue();
		msg.addReaction(Emoji.fromUnicode("⬅️")).queue();
		msg.addReaction(Emoji.fromUnicode("➡️")).queue();
	}

	public static Menu buildMenu(Message message, String content) {
		return buildMenu(message, content, 2000);
	}

	public static Menu buildMenu(Message message, String content, int maxPageLength) {
		List<Page> pages = new ArrayList<>();

		String[] split = SplitUtil.split(content, maxPageLength, SplitUtil.Strategy.NEWLINE).toArray(String[]::new);

		Arrays.stream(split).forEachOrdered(s -> {
			pages.add(new Page(MessageCreateData.fromContent(s)));
		});

		return new Menu(message, pages.toArray(new Page[0]));
	}

	public static Menu buildMenu(Message message, MessageCreateData... pages) {
		return new Menu(message, Arrays.stream(pages).map(Page::new).toArray(Page[]::new));
	}

	public static void removeMenu(Message msg) {
		currentMenus = currentMenus.stream().filter(
				menu -> !menu.message.getId().equals(msg.getId())
		).toList();
	}

	public static Menu getMenu(Message msg) {
		final Menu[] m = {null};
		currentMenus.forEach(menu -> {
			if (menu.message.getId().equals(msg.getId())) {
				m[0] = menu;
			}
		});
		return m[0];
	}

	public static void incrementPage(Message msg) {
		Menu menu = getMenu(msg);
		if (menu == null) return;
		if (menu.page + 1 < menu.pages.size())
			menu.page++;
		else menu.page = 0;
	}

	public static void decrementPage(Message msg) {
		Menu menu = getMenu(msg);
		if (menu == null) return;
		if (menu.page - 1 >= 0)
			menu.page--;
		else menu.page = menu.pages.size() - 1;
	}





	public static class Menu {
		public int page;
		public List<Page> pages;
		public Message message;

		public Menu(Message message, Page... pages) {
			this.message = message;
			this.page = 0;
			this.pages = Arrays.asList(pages);
		}
	}

	public static class Page {
		public MessageCreateData content;

		public Page(MessageCreateData content) {
			this.content = content;
		}
	}
}
