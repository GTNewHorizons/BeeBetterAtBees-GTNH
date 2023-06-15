package hellfirepvp.beebetteratbees.common;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/**
 * HellFirePvP@Admin
 * Date: 01.05.2016 / 22:46
 * on BeeBetterAtBees
 * ModConfig
 */
public class ModConfig {

    private static Configuration config;

    public static boolean shouldShowSecretRecipes = false;
    public static boolean showDuplicateTrees = false;

    private ModConfig() {}

    public static void init(File file) {
        config = new Configuration(file);

        config.load();

        loadFromConfig();

        config.save();
    }

    private static void loadFromConfig() {
        shouldShowSecretRecipes = config.getBoolean(
            "shouldShowSecretMutations",
            "general",
            false,
            "Set this to true, if you wish for the mod to show secret bee mutations.");
        showDuplicateTrees = config.getBoolean(
            "shouldShowDuplicateTrees",
            "general",
            true,
            "If this is set to false, it will not show the mutation tree for a specific bee if the same tree is already displayed.");
    }

    public static void save() {
        config.save();
    }
}
