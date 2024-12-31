/*
 * This file is part of Necrify (formerly Velocity Punishment), a plugin designed to manage player's punishments for the platforms Velocity and partly Paper.
 * Copyright (C) 2022-2024 JvstvsHD
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.jvstvshd.necrify.common.util;

import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.api.user.NecrifyUser;
import net.kyori.adventure.text.Component;

public class Paginator {

    private final PageProvider provider;
    private final int pages;
    private final MessageProvider messageProvider;

    public Paginator(PageProvider provider, int pages, MessageProvider messageProvider) {
        this.provider = provider;
        this.pages = pages;
        this.messageProvider = messageProvider;
    }

    public void showTo(NecrifyUser user, int startAtPage) {

    }

    public void showPageTo(NecrifyUser user, int page) {
        user.sendMessage(provider.createPage(page));
    }

    public Component surroundWithPaginating(Component component) {
        return Component.text().append(
                //Component.text("<<").clickEvent(ClickEvent)
                Component.newline(), component, Component.newline()).build();
    }

    public Component createHeader(int page) {
        return messageProvider.provide("paginator.header", Component.text(page), Component.text(pages));
    }

    public Component createFooter(int page) {
        return messageProvider.provide("paginator.footer", Component.text(page), Component.text(pages));
    }

    /**
     * A provider for the pages of the paginator. This provider is called whenever a new page is requested.
     * Only the real page contents should be created here, no pagination logic should be implemented here.
     * This is handled by the paginator itself.
     */
    public interface PageProvider {
        Component createPage(int page);
    }
}
