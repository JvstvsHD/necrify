/*
 * This file is part of Necrify (formerly Velocity Punishment), which is licensed under the MIT license.
 *
 * Copyright (c) 2022-2024 JvstvsHD
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.jvstvshd.necrify.velocity.message;

import de.jvstvshd.necrify.api.message.MessageProvider;
import de.jvstvshd.necrify.velocity.NecrifyVelocityPlugin;
import de.jvstvshd.necrify.velocity.config.ConfigData;
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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.PropertyResourceBundle;
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
            try (JarFile jar = new JarFile(new File(NecrifyVelocityPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI()))) {
                for (JarEntry translationEntry : jar.stream().filter(jarEntry -> jarEntry.getName().toLowerCase().contains("translations") && !jarEntry.isDirectory()).toList()) {
                    var path = Path.of(baseDir.toString(), translationEntry.getName().split("/")[1]);
                    if (Files.exists(path)) {
                        continue;
                    }
                    LOGGER.info("copying translation file {}", translationEntry.getName());
                    Files.copy(Objects.requireNonNull(NecrifyVelocityPlugin.class.getResourceAsStream("/" + translationEntry.getName())), path);
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



    private final ConfigData configData;
    private final boolean autoPrefixed;

    public ResourceBundleMessageProvider(@NotNull ConfigData configData) {
        this(configData, true);
    }

    private ResourceBundleMessageProvider(@NotNull ConfigData configData, boolean autoPrefixed) {
        this.configData = configData;
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
        return input == null ? configData.getDefaultLanguage() : input;
    }

    @Override
    public MessageProvider unprefixedProvider() {
        if (!autoPrefixed) {
            return this;
        }
        return new ResourceBundleMessageProvider(configData, false);
    }
}
