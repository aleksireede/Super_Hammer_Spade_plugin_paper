package io.github.aleksireede.hammerspade.common;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class Text {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private Text() {
    }

    public static Component miniMessage(final String value, final TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(value, resolvers);
    }

    public static String plain(final Component component) {
        return PLAIN.serialize(component);
    }
}
