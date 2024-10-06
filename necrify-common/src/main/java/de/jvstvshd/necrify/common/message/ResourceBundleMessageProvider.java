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

package de.jvstvshd.necrify.common.message;

import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.common.AbstractNecrifyPlugin;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class ResourceBundleMessageProvider implements MessageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceBundleMessageProvider.class);
    private static final Component PREFIX = MiniMessage.miniMessage().deserialize("<grey>[<gradient:#ff1c08:#ff3f2e>Necrify</gradient>]</grey> ");

    static {
        var registry = TranslationRegistry.create(Key.key("necrify"));
        registry.defaultLocale(Locale.ENGLISH);
        Path baseDir = null;
        try {
            baseDir = Path.of("plugins", "necrify", "translations");
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
            }
        } catch (IOException e) {
            LOGGER.error("An error occurred while creating the translation directory", e);
        }
        try (Stream<Path> paths = Files.list(baseDir)) {
            List<Path> registeredPaths = new ArrayList<>();
            try (JarFile jar = new JarFile(new File(AbstractNecrifyPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI()))) {
                for (JarEntry translationEntry : jar.stream().filter(jarEntry -> jarEntry.getName().toLowerCase().contains("translations") && !jarEntry.isDirectory()).toList()) {
                    var path = baseDir.resolve(translationEntry.getName().split("/")[1]);
                    var inputStream = Objects.requireNonNull(AbstractNecrifyPlugin.class.getResourceAsStream("/" + translationEntry.getName()));
                    if (Files.exists(path)) {
                        var existingBundle = new Properties();
                        existingBundle.load(Files.newInputStream(path));
                        var jarBundle = new PropertyResourceBundle(inputStream);
                        var jarBundleKeys = jarBundle.getKeys();
                        int changes = 0;
                        while (jarBundleKeys.hasMoreElements()) {
                            var key = jarBundleKeys.nextElement();
                            if (!existingBundle.containsKey(key)) {
                                existingBundle.put(key, jarBundle.getString(key));
                                changes++;
                            }
                        }
                        if (changes > 0) {
                            LOGGER.info("updating {} entries in translation file {}", changes, translationEntry.getName());
                            existingBundle.store(Files.newOutputStream(path), null);
                        }
                        continue;
                    }
                    LOGGER.info("copying translation file {}", translationEntry.getName());
                    Files.copy(inputStream, path);
                    registeredPaths.add(path);
                }
            }
            registerFrom(paths, registry);
            try (Stream<Path> registeredStream = registeredPaths.stream()) {
                registerFrom(registeredStream, registry);
            }
            GlobalTranslator.translator().addSource(registry);
        } catch (Exception e) {
            LOGGER.error("An error occurred while loading translations", e);
        }
    }

    private static void registerFrom(Stream<Path> paths, TranslationRegistry registry) {
        paths.filter(path -> path.getFileName().toString().endsWith(".properties")).forEach(path -> {
            PropertyResourceBundle resource;
            try {
                resource = new PropertyResourceBundle(Files.newInputStream(path));
                var locale = locale(path.getFileName().toString());
                registry.registerAll(locale, resource, false);

            } catch (IOException e) {
                LOGGER.error("An error occurred while loading translation file {}", path.getFileName(), e);
            }
        });
    }

    private static Locale locale(String fileName) {
        return Objects.requireNonNull(Translator.parseLocale(fileName.substring(0, fileName.length() - ".properties".length())));
    }


    private final Locale defaultLocale;
    private final boolean autoPrefixed;

    public ResourceBundleMessageProvider(@NotNull Locale defaultLocale) {
        this(defaultLocale, true);
    }

    private ResourceBundleMessageProvider(@NotNull Locale defaultLocale, boolean autoPrefixed) {
        this.defaultLocale = defaultLocale;
        this.autoPrefixed = autoPrefixed;
    }

    @Override
    public @NotNull Component provide(@NotNull String key, @Nullable Locale locale, Component... args) {
        return GlobalTranslator.render(provide(key, args), orDefault(locale));
    }

    @Override
    public @NotNull Component provide(@NotNull String key, Component... args) {
        Objects.requireNonNull(key, "key may not be null");
        return prefixed(Component.translatable(key, args));
    }

    @Override
    public @NotNull Component internalError(@Nullable Locale locale) {
        return provide("error.internal", locale).color(NamedTextColor.DARK_RED);
    }

    @Override
    public @NotNull Component internalError() {
        return provide("error.internal").color(NamedTextColor.DARK_RED);
    }

    @Override
    public @NotNull Component prefix() {
        return PREFIX;
    }

    @Override
    public boolean autoPrefixed() {
        return autoPrefixed;
    }

    @NotNull
    private Locale orDefault(@Nullable Locale input) {
        return input == null ? defaultLocale : input;
    }

    @Override
    public MessageProvider unprefixedProvider() {
        if (!autoPrefixed) {
            return this;
        }
        return new ResourceBundleMessageProvider(defaultLocale, false);
    }
}
