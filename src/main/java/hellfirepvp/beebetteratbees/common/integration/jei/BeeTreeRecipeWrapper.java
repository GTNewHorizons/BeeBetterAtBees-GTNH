package hellfirepvp.beebetteratbees.common.integration.jei;

import com.google.common.collect.Maps;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBeeMutation;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import hellfirepvp.beebetteratbees.client.util.SimpleBinaryTree;
import hellfirepvp.beebetteratbees.common.data.Config;
import hellfirepvp.beebetteratbees.common.util.BeeUtil;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This class is part of the BeeBetterAtBees Mod
 * Class: BeeTreeRecipeWrapper
 * Created by HellFirePvP
 * Date: 11.10.2018 / 20:20
 */
public class BeeTreeRecipeWrapper implements IRecipeWrapper {

    private static final int MIN_X = 20, MAX_X = 170;
    private static final int X_SEPERATION_THRESHOLD = 7;
    private static final int Y_OFFSET = 0;

    private SimpleBinaryTree<IAlleleBeeSpecies> mutationTree;
    private List<PosItemStack> evaluatedBeePositions;
    private PosItemStack rootStack;

    public BeeTreeRecipeWrapper(IAlleleBeeSpecies rootBee, int select) {
        this.mutationTree = new SimpleBinaryTree<>(4, rootBee,
                new SimpleBinaryTree.RootProvider<IAlleleBeeSpecies>() {
                    @Override
                    public IAlleleBeeSpecies[] provideSubNodes(IAlleleBeeSpecies superNode) {
                        List<IBeeMutation> mutations = BeeUtil.getMutationsWithResult(superNode);
                        if(mutations.size() > 0) {
                            IBeeMutation mutation = mutations.get(superNode == rootBee ? MathHelper.clamp(select, 0, mutations.size() - 1) : 0);

                            return new IAlleleBeeSpecies[] {
                                    (IAlleleBeeSpecies) mutation.getAllele0(),
                                    (IAlleleBeeSpecies) mutation.getAllele1()
                            };
                        } else {
                            return null;
                        }
                    }
                });


        if(!Config.showDuplicateTrees) {
            List<IAlleleBeeSpecies> foundMutationTrees = new ArrayList<>();
            removeAndReplaceDuplicates(this.mutationTree.getRoot(), foundMutationTrees);
        }

        //Important: We don't need to buffer root, because that's the "result"
        this.evaluatedBeePositions = new LinkedList<>();
        int iterationDepth = 3;
        int maxTotalDepth = Math.min(iterationDepth, mutationTree.getRoot().getMaxFollowingDepth());
        if(maxTotalDepth <= 0) {
            return; //In case the root is a leaf, there is nothing to display anyway except the root.
        }
        int yStep = 110 / maxTotalDepth;

        boolean oversized = checkSeparationWidth(maxTotalDepth, X_SEPERATION_THRESHOLD);
        int evaluatedMaxX = /*this.oversized ? buildNewMaxX(maxTotalDepth, X_SEPERATION_THRESHOLD) :*/ MAX_X;

        int center = (MIN_X + evaluatedMaxX) / 2;
        PosItemStack leftChild = placeInRenderBuffer(mutationTree.getRoot().getLeftNode(),
                Y_OFFSET + yStep, yStep,
                MIN_X, center, iterationDepth - 1);
        PosItemStack rightChild = placeInRenderBuffer(mutationTree.getRoot().getRightNode(),
                Y_OFFSET + yStep, yStep,
                center, evaluatedMaxX, iterationDepth - 1);

        List<IBeeMutation> mutationsToRoot = BeeUtil.getMutationsWithResult(mutationTree.getRoot().getValue());
        float ch = -1;
        List<String> requirements = new LinkedList<>();
        if(mutationsToRoot.size() > 0) {
            IBeeMutation mut = mutationsToRoot.get(0);
            ch = mut.getBaseChance();
            try {
                requirements.addAll(mut.getSpecialConditions());
            } catch (Throwable tr) {}
        }

        this.rootStack = new PosItemStack(
                mutationTree.getRoot().getValue(),
                (MIN_X + evaluatedMaxX) / 2, Y_OFFSET,
                ch, requirements,
                leftChild, rightChild, true);
    }

    private void removeAndReplaceDuplicates(SimpleBinaryTree.Node<IAlleleBeeSpecies> node, List<IAlleleBeeSpecies> discoveredMutations) { //Replace with 2 lines.
        if(discoveredMutations.contains(node.getValue())) {
            List<IBeeMutation> mutations = BeeUtil.getMutationsWithResult(node.getValue());
            if(mutations.size() > 0) node.removeDuplicate(); //Only if it actually has mutations that has this node as result.
        } else {
            discoveredMutations.add(node.getValue());
            if(node.getMaxFollowingDepth() > 0) {
                removeAndReplaceDuplicates(node.getLeftNode(), discoveredMutations);
                removeAndReplaceDuplicates(node.getRightNode(), discoveredMutations);
            }
        }
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<ItemStack> inputs = new LinkedList<>();
        this.evaluatedBeePositions.forEach(i -> inputs.add(BeeUtil.createStack(i.beeSpecies, EnumBeeType.DRONE)));
        this.evaluatedBeePositions.forEach(i -> inputs.add(BeeUtil.createStack(i.beeSpecies, EnumBeeType.PRINCESS)));
        this.evaluatedBeePositions.forEach(i -> inputs.add(BeeUtil.createStack(i.beeSpecies, EnumBeeType.QUEEN)));
        this.evaluatedBeePositions.forEach(i -> inputs.add(BeeUtil.createStack(i.beeSpecies, EnumBeeType.LARVAE)));
        ingredients.setInputs(ItemStack.class, inputs);

        List<ItemStack> outputs = new LinkedList<>();
        outputs.add(BeeUtil.createStack(this.rootStack.beeSpecies, EnumBeeType.DRONE));
        outputs.add(BeeUtil.createStack(this.rootStack.beeSpecies, EnumBeeType.PRINCESS));
        outputs.add(BeeUtil.createStack(this.rootStack.beeSpecies, EnumBeeType.QUEEN));
        outputs.add(BeeUtil.createStack(this.rootStack.beeSpecies, EnumBeeType.LARVAE));
        ingredients.setOutputs(ItemStack.class, outputs);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        Map<Rectangle, List<String>> boxes = Maps.newHashMap();
        drawExtrasFrom(this.rootStack, boxes);
        for (Map.Entry<Rectangle, List<String>> etr : boxes.entrySet()) {
            if (etr.getKey().contains(mouseX, mouseY)) {
                GlStateManager.disableDepth();
                GuiUtils.drawHoveringText(etr.getValue(), mouseX, mouseY, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, minecraft.fontRenderer);
                GlStateManager.enableDepth();
            }
        }
    }

    private static final Color lineColorBlack = new Color(0, 0, 0),
            lineColorRed = new Color(169, 0, 10);

    private static final int offsetCorrection = 8;
    private static final int possibleChildOffset = 16;

    public static void drawExtrasFrom(PosItemStack nodeStack, Map<Rectangle, List<String>> infoBoxes) {
        int nodeX = nodeStack.posX;
        int nodeY = nodeStack.posY;

        if(nodeStack.leftChild == null ||
                nodeStack.rightChild == null) {
            if(nodeStack.hasPossibleChildren) {

                Color drawColor = lineColorBlack;

                if(nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                    drawColor = lineColorRed;
                }

                float chance = nodeStack.baseChance;
                if(chance > 0) {
                    int length = drawChanceInfo(nodeX, nodeY, chance, Minecraft.getMinecraft().fontRenderer, drawColor);
                    if(nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                        Rectangle rec = new Rectangle(nodeX + 4, nodeY + 17, (int) (length * 0.75F), 5);
                        infoBoxes.put(rec, nodeStack.requirements);
                    }
                }

                drawLine(
                        nodeX + offsetCorrection, nodeY + offsetCorrection,
                        nodeX + offsetCorrection - 4,
                        nodeY + offsetCorrection + possibleChildOffset,
                        drawColor);
                drawLine(
                        nodeX + offsetCorrection, nodeY + offsetCorrection,
                        nodeX + offsetCorrection + 4,
                        nodeY + offsetCorrection + possibleChildOffset,
                        drawColor);
            }
            return; //Has no children anymore.
        }

        Color drawColor = lineColorBlack;

        if(nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
            drawColor = lineColorRed;
        }

        float chance = nodeStack.baseChance;
        if(chance > 0) {
            int length = drawChanceInfo(nodeX, nodeY, chance, Minecraft.getMinecraft().fontRenderer, drawColor);
            if(nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                Rectangle rec = new Rectangle(nodeX + 4, nodeY + 17, (int) (length * 0.75F), 5);
                infoBoxes.put(rec, nodeStack.requirements);
            }
        }

        PosItemStack left = nodeStack.leftChild;
        drawLine(
                nodeX + offsetCorrection, nodeY + offsetCorrection,
                left.posX + offsetCorrection, left.posY + offsetCorrection,
                drawColor);
        PosItemStack right = nodeStack.rightChild;
        drawLine(
                nodeX + offsetCorrection, nodeY + offsetCorrection,
                right.posX + offsetCorrection, right.posY + offsetCorrection,
                drawColor);

        drawExtrasFrom(left, infoBoxes);
        drawExtrasFrom(right, infoBoxes);
    }

    private static int drawChanceInfo(int nodeX, int nodeY, float chance, FontRenderer fr, Color drawColor) {
        GL11.glPushMatrix();
        GL11.glTranslatef(nodeX + 4, nodeY + 17, 0);
        GL11.glScalef(0.65F, 0.65F, 0.65F);
        int chAsInt = (int) (chance);
        StringBuilder sb = new StringBuilder().append(TextFormatting.BOLD);
        if(chAsInt <= 0) {
            sb.append("<1%");
        } else {
            sb.append(chAsInt).append("%");
        }
        int length = fr.drawString(sb.toString(), 0, 0, drawColor.getRGB());
        GL11.glPopMatrix();
        return length;
    }

    private static void drawLine(double lx, double ly, double hx, double hy, Color color) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(3.0F);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GL11.glColor4f(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, 0.4F);

        Tessellator t = Tessellator.getInstance();
        BufferBuilder vb = t.getBuffer();
        vb.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        vb.pos(lx, ly, 0).endVertex();
        vb.pos(hx, hy, 0).endVertex();
        t.draw();

        GL11.glColor4f(1F, 1F, 1F, 1F);
        GlStateManager.color(1F, 1F, 1F, 1F);

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public List<PosItemStack> getEvaluatedBeePositions() {
        return evaluatedBeePositions;
    }

    public PosItemStack getRootStack() {
        return rootStack;
    }

    private boolean checkSeparationWidth(int maxTotalDepth, int xSeparationThreshold) {
        double maxDivision = Math.pow(2, maxTotalDepth + 1);
        int resultingLLWidth = (int) (120 / maxDivision); //LowestLevelWidth
        return resultingLLWidth < xSeparationThreshold;
    }

    private PosItemStack placeInRenderBuffer(SimpleBinaryTree.Node<IAlleleBeeSpecies> node, int minY, int yStep, int minX, int maxX, int iterationMaxCount) {
        int center = (minX + maxX) / 2;
        iterationMaxCount--;
        if(iterationMaxCount < 0 || node.getMaxFollowingDepth() <= 0) {
            List<IBeeMutation> mutationsToRoot = BeeUtil.getMutationsWithResult(node.getValue());
            float ch = -1;
            List<String> requirements = new LinkedList<>();
            if(mutationsToRoot.size() > 0) {
                IBeeMutation mut = mutationsToRoot.get(0);
                ch = mut.getBaseChance();
                try {
                    requirements.addAll(mut.getSpecialConditions());
                } catch (Throwable tr) {
                    requirements = new LinkedList<>();
                }
            }
            PosItemStack leaf = new PosItemStack( //Leaf
                    node.getValue(),
                    center, minY,
                    ch, requirements,
                    null, null, node.getMaxFollowingDepth() > 0);
            evaluatedBeePositions.add(leaf);
            return leaf;
        }


        List<IBeeMutation> mutations = BeeUtil.getMutationsWithResult(node.getValue());
        float ch = -1;
        List<String> requirements = new LinkedList<>();
        if(mutations.size() > 0) {
            IBeeMutation mut = mutations.get(0);
            ch = mut.getBaseChance();
            try {
                requirements.addAll(mut.getSpecialConditions());
            } catch (Throwable tr) {
                requirements = new LinkedList<>();
            }
        }

        PosItemStack outNode = new PosItemStack(
                node.getValue(),
                center, minY,
                ch, requirements,
                node.getLeftNode() != null ?
                        placeInRenderBuffer(node.getLeftNode(),
                                minY + yStep, yStep,
                                minX, center, iterationMaxCount)
                        : null,
                node.getRightNode() != null ?
                        placeInRenderBuffer(node.getRightNode(),
                                minY + yStep, yStep,
                                center, maxX, iterationMaxCount)
                        : null,
                true);
        evaluatedBeePositions.add(outNode);
        return outNode;
    }

    public static class PosItemStack {

        public final IAlleleBeeSpecies beeSpecies;
        public final int posX, posY;

        public final boolean hasPossibleChildren;
        public final float baseChance;
        public final List<String> requirements;
        public final PosItemStack leftChild, rightChild;

        public PosItemStack(IAlleleBeeSpecies species, int x, int y,
                                           float baseChance, List<String> requirementInfo,
                                           PosItemStack leftChild, PosItemStack rightChild,
                                           boolean hasPossibleChildren) {
            this.beeSpecies = species;
            this.posX = x;
            this.posY = y;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.hasPossibleChildren = hasPossibleChildren;
            this.baseChance = baseChance;
            this.requirements = requirementInfo;
        }

    }

}
