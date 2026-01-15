package hellfirepvp.beebetteratbees.client.gui;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

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

    /**
     * Tooltip rectangles, where list index is the recipe index of this page
     */
    private final ArrayList<Map<Rectangle, Collection<String>>> tipBoxes = new ArrayList<>(2); // 1-2 recipes/pg

    @Override
    public void drawExtras(int recipe) {
        CachedRecipe rec = this.arecipes.get(recipe);
        if (rec instanceof CachedBeeMutationTree cachedTree) {
            Map<Rectangle, Collection<String>> boxes = new HashMap<>(4);
            CachedBeeMutationTree.PositionedMutationNodeStack root = cachedTree.getRootStack();
            drawExtrasFrom(root, boxes);
            this.tipBoxes.add(boxes);
        }
    }

    private static final Color lineColorBlack = new Color(0, 0, 0), lineColorRed = new Color(169, 0, 10);

    private static final int offsetCorrection = 8;
    private static final int possibleChildOffset = 16;

    public static void drawExtrasFrom(CachedBeeMutationTree.PositionedMutationNodeStack nodeStack,
        Map<Rectangle, Collection<String>> infoBoxes) {
        int nodeX = nodeStack.relx;
        int nodeY = nodeStack.rely;

        if (nodeStack.leftChild == null || nodeStack.rightChild == null) {
            if (nodeStack.hasPossibleChildren) {

                Color drawColor = lineColorBlack;

                if (nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                    drawColor = lineColorRed;
                }

                float chance = nodeStack.baseChance;
                if (chance > 0) {
                    int length = drawChanceInfo(nodeX, nodeY, chance, Minecraft.getMinecraft().fontRenderer, drawColor);
                    if (nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                        Rectangle rec = new Rectangle(nodeX + 4, nodeY + 17, (int) (length * 0.75F), 5);
                        infoBoxes.put(rec, nodeStack.requirements);
                    }
                }

                drawLine(
                    nodeX + offsetCorrection,
                    nodeY + offsetCorrection,
                    nodeX + offsetCorrection - 4,
                    nodeY + offsetCorrection + possibleChildOffset,
                    drawColor);
                drawLine(
                    nodeX + offsetCorrection,
                    nodeY + offsetCorrection,
                    nodeX + offsetCorrection + 4,
                    nodeY + offsetCorrection + possibleChildOffset,
                    drawColor);
            }
            return; // Has no children anymore.
        }

        Color drawColor = lineColorBlack;

        if (nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
            drawColor = lineColorRed;
        }

        float chance = nodeStack.baseChance;
        if (chance > 0) {
            int length = drawChanceInfo(nodeX, nodeY, chance, Minecraft.getMinecraft().fontRenderer, drawColor);
            if (nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                Rectangle rec = new Rectangle(nodeX + 4, nodeY + 17, (int) (length * 0.75F), 5);
                infoBoxes.put(rec, nodeStack.requirements);
            }
        }

        CachedBeeMutationTree.PositionedMutationNodeStack left = nodeStack.leftChild;
        drawLine(
            nodeX + offsetCorrection,
            nodeY + offsetCorrection,
            left.relx + offsetCorrection,
            left.rely + offsetCorrection,
            drawColor);
        CachedBeeMutationTree.PositionedMutationNodeStack right = nodeStack.rightChild;
        drawLine(
            nodeX + offsetCorrection,
            nodeY + offsetCorrection,
            right.relx + offsetCorrection,
            right.rely + offsetCorrection,
            drawColor);

        drawExtrasFrom(left, infoBoxes);
        drawExtrasFrom(right, infoBoxes);
    }

    private static int drawChanceInfo(int nodeX, int nodeY, float chance, FontRenderer fr, Color drawColor) {
        GL11.glPushMatrix();
        GL11.glTranslatef(nodeX + 4, nodeY + 17, 0);
        GL11.glScalef(0.65F, 0.65F, 0.65F);
        int chAsInt = (int) (chance);
        StringBuilder sb = new StringBuilder().append(EnumChatFormatting.BOLD);
        if (chAsInt <= 0) {
            sb.append("<1%");
        } else {
            sb.append(chAsInt)
                .append("%");
        }
        int length = fr.drawString(sb.toString(), 0, 0, drawColor.getRGB());
        GL11.glPopMatrix();
        return length;
    }

    @Override
    public List<String> handleTooltip(GuiRecipe<?> gui, List<String> currenttip, int recipe) {
        if (GuiContainerManager.shouldShowTooltip(gui) && currenttip.isEmpty()) {
            Point pos = GuiDraw.getMousePosition();
            Point guiOffset = new Point(gui.guiLeft, gui.guiTop);
            Point recipeOffset = gui.getRecipePosition(recipe);
            Point relMouse = new Point(pos.x - guiOffset.x - recipeOffset.x, pos.y - guiOffset.y - recipeOffset.y);
            for (Rectangle rec : this.tipBoxes.get(recipe)
                .keySet()) {
                if (rec.contains(relMouse)) {
                    return new LinkedList<>(
                        this.tipBoxes.get(recipe)
                            .get(rec));
                }
            }
        }
        return super.handleTooltip(gui, currenttip, recipe);
    }

    private static void drawLine(double lx, double ly, double hx, double hy, Color color) {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(3.0F);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tes = Tessellator.instance;
        tes.startDrawing(GL11.GL_LINE_STRIP);
        tes.setColorRGBA(color.getRed(), color.getGreen(), color.getBlue(), 127);
        tes.addVertex(lx, ly, 0);
        tes.addVertex(hx, hy, 0);
        tes.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0F);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
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
