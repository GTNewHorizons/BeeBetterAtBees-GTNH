package hellfirepvp.beebetteratbees.common.integration;

import com.google.common.collect.Maps;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBeeMutation;
import forestry.api.apiculture.IBeeRoot;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAlleleRegistry;
import forestry.api.genetics.ISpeciesRoot;
import forestry.apiculture.ModuleApiculture;
import forestry.apiculture.items.ItemBeeGE;
import forestry.apiculture.items.ItemRegistryApiculture;
import hellfirepvp.beebetteratbees.common.data.Config;
import hellfirepvp.beebetteratbees.common.integration.jei.BeeTreeRecipeWrapper;
import hellfirepvp.beebetteratbees.common.integration.jei.CategoryBeeTree;
import hellfirepvp.beebetteratbees.common.util.BeeUtil;
import mezz.jei.api.*;
import mezz.jei.api.ingredients.IModIngredientRegistration;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class is part of the BeeBetterAtBees Mod
 * Class: ModIntegrationJEI
 * Created by HellFirePvP
 * Date: 11.10.2018 / 20:18
 */
@JEIPlugin
public class ModIntegrationJEI implements IModPlugin {

    public static IBeeRoot beeRoot;

    public static final String idBeeTree = "beebetteratbees.beetree";

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();

        registry.addRecipeCategories(new CategoryBeeTree(guiHelper));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        ISpeciesRoot root = AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
        if (root == null || !(root instanceof IBeeRoot)) {
            throw new IllegalStateException("Forestry bee root not enabled! Is forestry not installed or the bee module not enabled?");
        }
        beeRoot = (IBeeRoot) root;

        Map<String, Integer> foundRoots = Maps.newHashMap();
        for (IBeeMutation possibleMutation : beeRoot.getMutations(false)) {
            if (!Config.shouldShowSecretRecipes && possibleMutation.isSecret()) {
                return;
            }
            IAlleleBeeSpecies rootOfMutation = BeeUtil.getMutationRoot(possibleMutation);
            int count = foundRoots.getOrDefault(rootOfMutation.getUID(), 0);
            jeiRuntime.getRecipeRegistry().addRecipe(new BeeTreeRecipeWrapper(rootOfMutation, count), idBeeTree);
            count++;
            foundRoots.put(rootOfMutation.getUID(), count);
        }
    }

}
