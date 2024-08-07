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
package de.jvstvshd.necrify.velocity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigurationManager {

    private final static Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    private final Path path;
    private final ObjectMapper objectMapper;
    private ConfigData configData = new ConfigData();

    public ConfigurationManager(Path path) {
        this.path = path;
        this.objectMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    }

    private void create() throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        boolean write = false;
        if (!Files.exists(path)) {
            Files.createFile(path);
            write = true;
        }
        try (FileChannel channel = FileChannel.open(path)) {
            if (channel.size() <= 0 || write) {
                migrateConfig();
                save();
            }
        }
    }

    private void migrateConfig() {
        var oldConfig = path.getParent().resolve("config.json");
        if (!Files.exists(oldConfig)) {
            return;
        }
        try {
            var oldConfigData = new ObjectMapper().readValue(oldConfig.toFile(), ConfigData.class);
            logger.info("Found old config file, migrating to new one....");
            configData = oldConfigData;
            logger.info("Successfully migrated old config file to {}", path.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Could not migrate old config file.", e);
        }
    }

    public void load() throws IOException {
        create();
        configData = objectMapper.readValue(path.toFile(), ConfigData.class);
    }

    public void save() throws IOException {
        try (var writer = objectMapper.writerWithDefaultPrettyPrinter().writeValues(path.toFile())) {
            writer.write(configData);
        }
    }

    public ConfigData getConfiguration() {
        return configData;
    }
}
