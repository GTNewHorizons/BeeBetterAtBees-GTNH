package hellfirepvp.beebetteratbees.common.data;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * This class is part of the BeeBetterAtBees Mod
 * Class: Config
 * Created by HellFirePvP
 * Date: 11.10.2018 / 06:38
 */
public class Config {

    private static Configuration config;

    public static boolean shouldShowSecretRecipes = false;
    public static boolean showDuplicateTrees = false;

    private Config() {}

    public static void init(File file) {
        config = new Configuration(file);
        config.load();

        loadFromConfig();

        save();
    }

    private static void loadFromConfig() {
        shouldShowSecretRecipes = config.getBoolean("shouldShowSecretMutations", "general", shouldShowSecretRecipes, "Set this to true, if you wish for the mod to show secret bee mutations.");
        showDuplicateTrees = config.getBoolean("shouldShowDuplicateTrees", "general", showDuplicateTrees, "If this is set to false, it will not show the mutation tree for a specific bee if the same tree is already displayed.");
    }

    public static void save() {
        config.save();
    }

}
