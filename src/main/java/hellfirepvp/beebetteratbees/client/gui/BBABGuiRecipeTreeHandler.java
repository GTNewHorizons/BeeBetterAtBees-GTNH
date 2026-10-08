package hellfirepvp.beebetteratbees.client.gui;

import java.awt.Point;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

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
import hellfirepvp.beebetteratbees.client.requirements.RequirementResolvers;
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

    private CachedBeeMutationTree draggedTree;
    private int dragButton = -1;
    private int lastMouseX;
    private int lastMouseY;
    private int activeViewportClips;

    // Viewport dimensions for the graph area
    private static final int VIEWPORT_X = 15;
    private static final int VIEWPORT_Y = 0;
    private static final int VIEWPORT_WIDTH = 120;
    private static final int VIEWPORT_HEIGHT = 100;

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

        if (outputId.equals("all")) {
            loadAllRecipes();
        } else if (outputId.equals("item")) {
            loadCraftingRecipes((ItemStack) results[0]);
        }
    }

    private void loadAllRecipes() {
        for (IBeeMutation mutation : speciesRoot.getMutations(false)) {
            if (!mutation.isSecret() || ModConfig.shouldShowSecretRecipes) {
                this.arecipes.add(new CachedBeeMutationTree(mutation));
            }
        }
        cleanupDuplicateRecipes();
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
    public void drawBackground(int recipe) {
        super.drawBackground(recipe);
        pushViewportClip();
    }

    @Override
    public void drawForeground(int recipe) {
        try {
            super.drawForeground(recipe);
        } finally {
            popViewportClip();
        }
    }

    @Override
    public void drawExtras(int recipe) {
        CachedRecipe rec = this.arecipes.get(recipe);
        if (!(rec instanceof CachedBeeMutationTree cachedTree)) return;

        cachedTree.getMutationNodesToRender()
            .forEach(node -> node.renderNode(cachedTree.getGraphState()));
    }

    private void pushViewportClip() {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
        int scaleFactor = resolution.getScaleFactor();

        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrix);
        float scaleX = matrix.get(0);
        float scaleY = matrix.get(5);
        float translateX = matrix.get(12);
        float translateY = matrix.get(13);
        int left = (int) Math.floor((VIEWPORT_X * scaleX + translateX) * scaleFactor);
        int right = (int) Math.ceil(((VIEWPORT_X + VIEWPORT_WIDTH) * scaleX + translateX) * scaleFactor);
        int top = (int) Math.floor((VIEWPORT_Y * scaleY + translateY) * scaleFactor);
        int bottom = (int) Math.ceil(((VIEWPORT_Y + VIEWPORT_HEIGHT) * scaleY + translateY) * scaleFactor);
        int clipX = left;
        int clipY = minecraft.displayHeight - bottom;
        int clipWidth = right - left;
        int clipHeight = bottom - top;
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        IntBuffer currentScissor = BufferUtils.createIntBuffer(4);
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, currentScissor);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_SCISSOR_BIT);
        activeViewportClips++;
        // snipsnip
        if (scissorEnabled) {
            int currentX = currentScissor.get(0);
            int currentY = currentScissor.get(1);
            int currentRight = currentX + currentScissor.get(2);
            int currentTop = currentY + currentScissor.get(3);
            int intersectRight = Math.min(clipX + clipWidth, currentRight);
            int intersectTop = Math.min(clipY + clipHeight, currentTop);
            clipX = Math.max(clipX, currentX);
            clipY = Math.max(clipY, currentY);
            clipWidth = Math.max(0, intersectRight - clipX);
            clipHeight = Math.max(0, intersectTop - clipY);
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(clipX, clipY, clipWidth, clipHeight);
    }

    private void popViewportClip() {
        if (activeViewportClips <= 0) return;
        activeViewportClips--;
        GL11.glPopAttrib();
    }

    @Override
    public boolean mouseClicked(GuiRecipe<?> gui, int button, int recipe) {
        Point mousePos = GuiDraw.getMousePosition();
        Point localPos = getLocalRecipePosition(gui, recipe, mousePos);
        if ((button == 0 || button == 2) && isInsideViewport(localPos)
            && this.arecipes.get(recipe) instanceof CachedBeeMutationTree cachedTree) {
            draggedTree = cachedTree;
            dragButton = button;
            lastMouseX = mousePos.x;
            lastMouseY = mousePos.y;
            return true;
        }

        return super.mouseClicked(gui, button, recipe);
    }

    @Override
    public boolean mouseScrolled(GuiRecipe<?> gui, int scroll, int recipe) {
        if (scroll == 0) return false;

        Point localPos = getLocalRecipePosition(gui, recipe, GuiDraw.getMousePosition());
        if (isInsideViewport(localPos) && this.arecipes.get(recipe) instanceof CachedBeeMutationTree cachedTree) {
            cachedTree.zoomViewport(localPos.x, localPos.y, scroll);
            return true;
        }

        return false;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (draggedTree == null) {
            return;
        }
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiRecipe<?>) || !Mouse.isButtonDown(dragButton)) {
            draggedTree = null;
            dragButton = -1;
            return;
        }

        Point mousePos = GuiDraw.getMousePosition();
        draggedTree.panViewport(mousePos.x - lastMouseX, mousePos.y - lastMouseY);
        lastMouseX = mousePos.x;
        lastMouseY = mousePos.y;
    }

    @Override
    public List<String> handleTooltip(GuiRecipe<?> gui, List<String> currenttip, int recipe) {
        if (!(this.arecipes.get(recipe) instanceof CachedBeeMutationTree cachedTree)) {
            return super.handleTooltip(gui, currenttip, recipe);
        }
        if (GuiContainerManager.shouldShowTooltip(gui) && currenttip.isEmpty()) {
            Point pos = GuiDraw.getMousePosition();
            Point localPos = getLocalRecipePosition(gui, recipe, pos);

            if (isInsideViewport(localPos)) {
                for (IMutationNode mutation : cachedTree.getMutationNodesToRender()) {
                    if (mutation instanceof ChanceInfoNode chanceInfoNode
                        && chanceInfoNode.containsPoint(localPos.x, localPos.y, cachedTree.getGraphState())) {
                        return new LinkedList<>(chanceInfoNode.infoLines);
                    }
                }
            }
        }
        return super.handleTooltip(gui, currenttip, recipe);
    }

    private Point getLocalRecipePosition(GuiRecipe<?> gui, int recipe, Point screenPosition) {
        Point recipeOffset = gui.getRecipePosition(recipe);
        return new Point(
            screenPosition.x - gui.guiLeft - recipeOffset.x,
            screenPosition.y - gui.guiTop - recipeOffset.y);
    }

    private boolean isInsideViewport(Point point) {
        return point.x >= VIEWPORT_X && point.x < VIEWPORT_X + VIEWPORT_WIDTH
            && point.y >= VIEWPORT_Y
            && point.y < VIEWPORT_Y + VIEWPORT_HEIGHT;
    }

    @Override
    public int getRecipeHeight(int recipe) {
        return ((CachedBeeMutationTree) this.arecipes.get(recipe)).getRecipeHeight();
    }

    @Override
    public void loadUsageRecipes(String inputId, Object... ingredients) {
        if (speciesRoot == null) return;

        if (inputId.equals("all")) {
            loadAllRecipes();
        } else if (inputId.equals("item")) {
            loadUsageRecipes((ItemStack) ingredients[0]);
        }
    }

    public void loadUsageRecipes(ItemStack ingredient) {
        if (speciesRoot == null) return;

        IAlleleSpecies species = null;
        if (speciesRoot.isMember(ingredient)) {
            IIndividual individual = speciesRoot.getMember(ingredient);
            if (individual != null && individual.getGenome() != null
                && individual.getGenome()
                    .getPrimary() != null) {
                species = individual.getGenome()
                    .getPrimary();
            }
        }

        for (IBeeMutation mutation : speciesRoot.getMutations(false)) {
            if (mutation.isSecret() && !ModConfig.shouldShowSecretRecipes) continue;
            boolean matchesSpecies = species != null && (mutation.getAllele0()
                .equals(species)
                || mutation.getAllele1()
                    .equals(species));
            if (matchesSpecies || RequirementResolvers.matches(mutation, ingredient)) {
                this.arecipes.add(new CachedBeeMutationTree(mutation));
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
