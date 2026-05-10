package org.antredesloutres.ottergames.managers;

import org.antredesloutres.ottergames.Main;
import org.antredesloutres.ottergames.models.GameConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class ConfigManager {
    private final Main plugin;
    private final File configFile;
    private FileConfiguration config;
    private final GameConfig gameConfig;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "games_config.yml");
        this.gameConfig = new GameConfig();
        load();
    }

    public void load() {
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create games_config.yml!");
            }
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        List<String> disabled = config.getStringList("disabled-games");
        gameConfig.setDisabledGames(new HashSet<>(disabled));
        gameConfig.setPreventSameGameConsecutively(config.getBoolean("prevent-same-game-consecutively", true));
        gameConfig.setMaxLives(config.getInt("max-lives", 3));
        gameConfig.setMinPlayersToContinue(config.getInt("min-players-to-continue", 1));
    }

    public void save() {
        config.set("disabled-games", List.copyOf(gameConfig.getDisabledGames()));
        config.set("prevent-same-game-consecutively", gameConfig.isPreventSameGameConsecutively());
        config.set("max-lives", gameConfig.getMaxLives());
        config.set("min-players-to-continue", gameConfig.getMinPlayersToContinue());
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save games_config.yml!");
        }
    }

    public GameConfig getGameConfig() {
        return gameConfig;
    }
}
