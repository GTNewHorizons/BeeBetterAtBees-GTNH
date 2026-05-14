package hellfirepvp.beebetteratbees.client.gui;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiRecipe;
import forestry.api.apiculture.IBeeMutation;
import forestry.api.apiculture.IBeeRoot;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IIndividual;
import forestry.api.genetics.ISpeciesRoot;
import hellfirepvp.beebetteratbees.client.gui.CachedBeeMutationTree.ChanceInfoNode;
import hellfirepvp.beebetteratbees.client.gui.CachedBeeMutationTree.IMutationNode;
import hellfirepvp.beebetteratbees.common.BeeBetterAtBees;
import hellfirepvp.beebetteratbees.common.ModConfig;

/**
 * HellFirePvP@Admin
 * Date: 28.04.2016 / 22:06
 * on BeeBetterAtBees
 * BBABGuiRecipeTreeHandler
 */
public class BBABGuiRecipeTreeHandler extends AbstractTreeGUIHandler {

    public static final int BEE_TYPE_PRINCESS = 1;
    public static final int BEE_TYPE_DRONE = 0;

    private static IBeeRoot speciesRoot;

    public static List<IBeeMutation> getMutationsWithResult(IAllele allele) {
        if (speciesRoot == null) return new LinkedList<>();
        LinkedList<IBeeMutation> out = new LinkedList<>();
        for (IBeeMutation mutation : speciesRoot.getMutations(false)) {
            if (mutation.getTemplate()[0].equals(allele)) out.add(mutation);
        }
        return out;
    }

    public static ItemStack createStack(IAlleleSpecies species, int type) {
        ISpeciesRoot root = species.getRoot();
        IAllele[] template = root.getTemplate(species.getUID());
        if (template == null) {
            BeeBetterAtBees.log.warn("Template for %s doesn't exist! Skipping...", species.getUID());
            return null;
        }
        IIndividual individual = root.templateAsIndividual(template);
        individual.analyze();
        ItemStack stack = root.getMemberStack(individual, type);
        if (stack == null) {
            BeeBetterAtBees.log.warn("Got no MemberStack back when creating bee (%s) ?", species.getUID());
        }
        return stack;
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (speciesRoot == null) return;

        if (outputId.equals("item")) {
            loadCraftingRecipes((ItemStack) results[0]);
        }
    }

    public void loadCraftingRecipes(ItemStack result) {
        if (speciesRoot == null) return;

        if (!speciesRoot.isMember(result)) {
            return;
        }
        IIndividual resultIndividual = speciesRoot.getMember(result);
        if (resultIndividual == null) {
            BeeBetterAtBees.log.warn("IIndividual is null searching recipe for %s", result.toString());
            return;
        }
        if (resultIndividual.getGenome() == null) {
            BeeBetterAtBees.log.warn("Genome is null when searching recipe for %s", result.toString());
            return;
        }
        if (resultIndividual.getGenome()
            .getPrimary() == null) {
            BeeBetterAtBees.log.warn("Species is null when searching recipe for %s", result.toString());
            return;
        }
        IAlleleSpecies species = resultIndividual.getGenome()
            .getPrimary();
        for (IBeeMutation mutation : speciesRoot.getMutations(false)) {
            if (mutation.getTemplate()[0].equals(species)) {
                if (!mutation.isSecret() || ModConfig.shouldShowSecretRecipes) {
                    this.arecipes.add(new CachedBeeMutationTree(mutation));
                }
            }
        }
        cleanupDuplicateRecipes();
    }

    @Override
    public void drawExtras(int recipe) {
        CachedRecipe rec = this.arecipes.get(recipe);
        if (rec instanceof CachedBeeMutationTree cachedTree) {
            cachedTree.getMutationNodesToRender()
                .forEach(node -> node.renderNode());
        }
    }

    @Override
    public List<String> handleTooltip(GuiRecipe<?> gui, List<String> currenttip, int recipe) {
        if (GuiContainerManager.shouldShowTooltip(gui) && currenttip.isEmpty()
            && this.arecipes.get(recipe) instanceof CachedBeeMutationTree cachedTree) {
            Point pos = GuiDraw.getMousePosition();
            Point guiOffset = new Point(gui.guiLeft, gui.guiTop);
            Point recipeOffset = gui.getRecipePosition(recipe);

            for (IMutationNode mutation : cachedTree.getMutationNodesToRender()) {
                if (mutation instanceof ChanceInfoNode chanceInfoNode && chanceInfoNode
                    .containsPoint(pos.x - guiOffset.x - recipeOffset.x, pos.y - guiOffset.y - recipeOffset.y)) {
                    return new LinkedList<>(chanceInfoNode.infoLines);
                }
            }

        }
        return super.handleTooltip(gui, currenttip, recipe);
    }

    @Override
    public void loadUsageRecipes(String inputId, Object... ingredients) {
        if (speciesRoot == null) return;

        if (inputId.equals("item")) {
            loadUsageRecipes((ItemStack) ingredients[0]);
        }
    }

    public void loadUsageRecipes(ItemStack ingredient) {
        if (speciesRoot == null) return;

        if (!speciesRoot.isMember(ingredient)) {
            return;
        }
        IIndividual individual = speciesRoot.getMember(ingredient);
        if (individual == null) {
            BeeBetterAtBees.log.warn("IIndividual is null searching recipe for %s", ingredient.toString());
            return;
        }
        if (individual.getGenome() == null) {
            BeeBetterAtBees.log.warn("Genome is null when searching recipe for %s", ingredient.toString());
            return;
        }
        if (individual.getGenome()
            .getPrimary() == null) {
            BeeBetterAtBees.log.warn("Species is null when searching recipe for %s", ingredient.toString());
            return;
        }
        IAlleleSpecies species = individual.getGenome()
            .getPrimary();
        for (IBeeMutation mutation : speciesRoot.getMutations(false)) {
            if (mutation.getAllele0()
                .equals(species)
                || mutation.getAllele1()
                    .equals(species)) {
                if (!mutation.isSecret() || ModConfig.shouldShowSecretRecipes) {
                    this.arecipes.add(new CachedBeeMutationTree(mutation));
                }
            }
        }
        cleanupDuplicateRecipes();
    }

    private void cleanupDuplicateRecipes() {
        for (CachedRecipe recipe : arecipes) {
            if (recipe instanceof CachedBeeMutationTree) {
                boolean clean = true;
                Iterator<CachedRecipe> iterator = arecipes.iterator();
                while (iterator.hasNext()) {
                    CachedRecipe recipeOther = iterator.next();
                    if (recipe == recipeOther) continue;
                    if (recipe.equals(recipeOther)) {
                        iterator.remove();
                        clean = false;
                    }
                }
                if (!clean) {
                    cleanupDuplicateRecipes();
                    break;
                }
            }
        }
    }

    @Override
    public String getGuiTexture() {
        return "beebetteratbees:textures/gui/neiBlank.png";
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("bbab.gui.breedtree");
    }

    public static void loadBeeRoot() {
        speciesRoot = (IBeeRoot) AlleleManager.alleleRegistry.getSpeciesRoot("rootBees");
        if (speciesRoot == null) {
            BeeBetterAtBees.log.warn("Bee Species Root not found, this mod has no use without it.");
        } else {
            BeeBetterAtBees.log.info("Bee Species Root found!");
        }
    }

}
