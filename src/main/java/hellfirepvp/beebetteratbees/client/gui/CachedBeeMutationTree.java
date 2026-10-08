package hellfirepvp.beebetteratbees.client.gui;

import static hellfirepvp.beebetteratbees.client.gui.BBABGuiRecipeTreeHandler.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import forestry.api.apiculture.IBeeMutation;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import hellfirepvp.beebetteratbees.client.gui.graph.GraphInteractionState;
import hellfirepvp.beebetteratbees.client.util.ColorUtils;
import hellfirepvp.beebetteratbees.client.util.SimpleBinaryTree;
import hellfirepvp.beebetteratbees.common.ModConfig;

/**
 * HellFirePvP@Admin
 * Date: 28.04.2016 / 23:44
 * on BeeBetterAtBees
 * CachedBeeMutationTree
 */
public class CachedBeeMutationTree extends CachedRecipe {

    protected static interface IMutationNode {

        boolean containsPoint(int x, int y, GraphInteractionState state);

        void renderNode(GraphInteractionState state);
    }

    protected static class LineNode implements IMutationNode {

        public final double lx;
        public final double ly;
        public final double hx;
        public final double hy;
        public final Color color;

        public LineNode(double lx, double ly, double hx, double hy, Color color) {
            this.lx = lx;
            this.ly = ly;
            this.hx = hx;
            this.hy = hy;
            this.color = color;
        }

        public boolean containsPoint(int x, int y, GraphInteractionState state) {
            return false;
        }

        public void renderNode(GraphInteractionState state) {
            if (state == null) return;
            float[] start = state.worldToScreen((float) lx, (float) ly);
            float[] end = state.worldToScreen((float) hx, (float) hy);

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
            tes.addVertex(start[0], start[1], 0);
            tes.addVertex(end[0], end[1], 0);
            tes.draw();

            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(2.0F);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }
    }

    protected static class ChanceInfoNode implements IMutationNode {

        public final int x;
        public final int y;
        public final float chance;
        public Color drawColor = LABEL_BLACK;
        public final String displayString;
        public Collection<String> infoLines;

        public ChanceInfoNode(int x, int y, PositionedMutationNodeStack nodeStack) {
            this.x = x;
            this.y = y;
            this.chance = nodeStack.baseChance;

            if (nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                this.drawColor = LABEL_RED;
                this.infoLines = nodeStack.requirements;
            } else {
                this.infoLines = Collections.emptyList();
            }

            if (chance < 1) {
                this.displayString = EnumChatFormatting.BOLD + "<1%";
            } else {
                this.displayString = EnumChatFormatting.BOLD.toString() + ((int) chance) + "%";
            }

        }

        public boolean containsPoint(int x, int y, GraphInteractionState state) {
            float[] drawPosition = state.worldToScreen(this.x + 8, this.y + 1);
            float drawX = drawPosition[0] - 8;
            float drawY = drawPosition[1];
            return x >= drawX && x <= drawX + 16 && y >= drawY && y <= drawY + 5;
        }

        public void renderNode(GraphInteractionState state) {
            if (state == null) return;
            float[] drawPosition = state.worldToScreen(this.x + 8, this.y + 1);
            GL11.glPushMatrix();
            GL11.glTranslatef(drawPosition[0], drawPosition[1], 0);
            GL11.glScalef(0.65F, 0.65F, 0.65F);
            GuiDraw.drawStringC(this.displayString, 0, 0, drawColor.getRGB(), false);
            GL11.glPopMatrix();
        }
    }

    // RENDERING BB
    // X = 15 to 135 (Size: 120)
    // Y = 10 to 100 (Size: 90)

    private static final int MIN_X = 15, MAX_X = 135;
    private static final int X_SEPERATION_THRESHOLD = 7;
    private static final int Y_OFFSET = 0;
    private static final Color LINE_BLACK = new Color(ColorUtils.neiLineBlack.getColor(), true);
    private static final Color LINE_RED = new Color(ColorUtils.neiLineRed.getColor(), true);
    private static final Color LABEL_BLACK = new Color(ColorUtils.neiLineLabelBlack.getColor(), true);
    private static final Color LABEL_RED = new Color(ColorUtils.neiLineLabelRed.getColor(), true);

    private static final int OFFSET_CORRECTION = 8;
    private static final int POSSIBLE_CHILD_OFFSET = 16;

    // viewport window the tree gets clipped + panned inside
    private static final int VIEW_X1 = 15;
    private static final int VIEW_Y1 = 0;
    private static final int VIEW_X2 = 135;
    private static final int VIEW_Y2 = 100;

    private final SimpleBinaryTree<IAllele> mutationTree;
    private final List<PositionedMutationNodeStack> evaluatedBeePositions;
    private int evaluatedMaxX;
    public final boolean oversized;
    private PositionedMutationNodeStack rootStack;
    private List<IMutationNode> mutationNodesToRender = new LinkedList<>();
    private final GraphInteractionState graphState = new GraphInteractionState();

    public void updateViewport() {
        if (rootStack == null) return;

        updateStackPosition(rootStack);

        for (PositionedMutationNodeStack stack : evaluatedBeePositions) {
            updateStackPosition(stack);
        }
    }

    private void updateStackPosition(PositionedMutationNodeStack stack) {
        float[] screenPosition = graphState.worldToScreen(stack.worldX + 8, stack.worldY + 8);
        stack.relx = Math.round(screenPosition[0] - 8);
        stack.rely = Math.round(screenPosition[1] - 8);
    }

    public void panViewport(float deltaX, float deltaY) {
        graphState.pan(deltaX, deltaY);
        clampViewport();
        updateViewport();
    }

    public void zoomViewport(int cursorX, int cursorY, int scroll) {
        float[] worldPosition = graphState.screenToWorld(cursorX, cursorY);
        float oldZoom = graphState.getZoomLevel();
        graphState.zoom(scroll > 0 ? 1 : -1);

        if (oldZoom == graphState.getZoomLevel()) return;

        graphState.setPan(
            cursorX - worldPosition[0] * graphState.getZoomLevel(),
            cursorY - worldPosition[1] * graphState.getZoomLevel());
        clampViewport();
        updateViewport();
    }

    private void clampViewport() {
        if (rootStack == null) return;

        int minContentX = rootStack.worldX;
        int maxContentX = rootStack.worldX + 16;
        int minContentY = rootStack.worldY;
        int maxContentY = rootStack.worldY + 32;

        for (PositionedMutationNodeStack stack : evaluatedBeePositions) {
            minContentX = Math.min(minContentX, stack.worldX);
            maxContentX = Math.max(maxContentX, stack.worldX + 16);
            minContentY = Math.min(minContentY, stack.worldY);
            maxContentY = Math.max(maxContentY, stack.worldY + 32);
        }

        float zoom = graphState.getZoomLevel();
        float minPanX = VIEW_X1 - maxContentX * zoom;
        float maxPanX = VIEW_X2 - minContentX * zoom;
        float minPanY = VIEW_Y1 - maxContentY * zoom;
        float maxPanY = VIEW_Y2 - minContentY * zoom;
        float panX = Math.max(minPanX, Math.min(maxPanX, graphState.getPanX()));
        float panY = Math.max(minPanY, Math.min(maxPanY, graphState.getPanY()));
        graphState.setPan(panX, panY);
    }

    public CachedBeeMutationTree(IBeeMutation parentMutation) {
        // parentMutation.getTemplate() Gets results primary at array[0], secondary at array[1]
        // parentMutation.getAllele0() or getAllele1() Gets bees needed for mutation.
        this.mutationTree = new SimpleBinaryTree<>(
            4,
            parentMutation.getTemplate()[0],
            new SimpleBinaryTree.RootProvider<>() {

                @Override
                public IAllele[] provideSubNodes(IAllele superNode) {
                    List<IBeeMutation> mutations = BBABGuiRecipeTreeHandler.getMutationsWithResult(superNode);
                    if (!mutations.isEmpty()) {
                        IBeeMutation mutation = mutations.get(0);

                        return new IAllele[] { mutation.getAllele0(), mutation.getAllele1() };
                    } else {
                        return null;
                    }
                }
            });

        if (!ModConfig.showDuplicateTrees) {
            List<IAllele> foundMutationTrees = new ArrayList<>();
            removeAndReplaceDuplicates(this.mutationTree.getRoot(), foundMutationTrees);
        }

        // Important: We don't need to buffer root, because that's the "result"
        this.evaluatedBeePositions = new LinkedList<>();
        int iterationDepth = 3;
        int maxTotalDepth = Math.min(
            iterationDepth,
            mutationTree.getRoot()
                .getMaxFollowingDepth());
        if (maxTotalDepth <= 0) {
            oversized = false;
            return; // In case the root is a leaf, there is nothing to display anyway except the root.
        }
        int yStep = 110 / maxTotalDepth;

        this.oversized = checkSeparationWidth(maxTotalDepth, X_SEPERATION_THRESHOLD);
        this.evaluatedMaxX = MAX_X;

        int center = (MIN_X + this.evaluatedMaxX) / 2;
        PositionedMutationNodeStack leftChild = placeInRenderBuffer(
            mutationTree.getRoot()
                .getLeftNode(),
            Y_OFFSET + yStep,
            yStep,
            MIN_X,
            center,
            iterationDepth - 1);
        PositionedMutationNodeStack rightChild = placeInRenderBuffer(
            mutationTree.getRoot()
                .getRightNode(),
            Y_OFFSET + yStep,
            yStep,
            center,
            this.evaluatedMaxX,
            iterationDepth - 1);

        List<IBeeMutation> mutationsToRoot = getMutationsWithResult(
            mutationTree.getRoot()
                .getValue());
        float ch = -1;
        Collection<String> requirements = new LinkedList<>();
        if (!mutationsToRoot.isEmpty()) {
            IBeeMutation mut = mutationsToRoot.get(0);
            ch = mut.getBaseChance();
            try {
                requirements = mut.getSpecialConditions();
            } catch (Throwable ignored) {}
        }

        this.rootStack = new PositionedMutationNodeStack(
            createStack(
                (IAlleleSpecies) mutationTree.getRoot()
                    .getValue(),
                BEE_TYPE_PRINCESS),
            (MIN_X + this.evaluatedMaxX) / 2,
            Y_OFFSET,
            ch,
            requirements,
            leftChild,
            rightChild,
            true);

        generateMutationNodes(rootStack);
        frameInitialGeneration();
    }

    private void frameInitialGeneration() {
        if (rootStack == null) return;

        List<PositionedMutationNodeStack> initialGeneration = new ArrayList<>();
        initialGeneration.add(rootStack);
        if (rootStack.leftChild != null) initialGeneration.add(rootStack.leftChild);
        if (rootStack.rightChild != null) initialGeneration.add(rootStack.rightChild);

        float minCenterX = Float.MAX_VALUE;
        float maxCenterX = -Float.MAX_VALUE;
        float minCenterY = Float.MAX_VALUE;
        float maxCenterY = -Float.MAX_VALUE;

        for (PositionedMutationNodeStack stack : initialGeneration) {
            float centerX = stack.worldX + 8;
            float centerY = stack.worldY + 8;
            minCenterX = Math.min(minCenterX, centerX);
            maxCenterX = Math.max(maxCenterX, centerX);
            minCenterY = Math.min(minCenterY, centerY);
            maxCenterY = Math.max(maxCenterY, centerY);
        }

        float availableWidth = VIEW_X2 - VIEW_X1 - 16;
        float availableHeight = VIEW_Y2 - VIEW_Y1 - 16;
        float width = maxCenterX - minCenterX;
        float height = maxCenterY - minCenterY;
        float zoom = 1.0F;

        if (width > 0) zoom = Math.min(zoom, availableWidth / width);
        if (height > 0) zoom = Math.min(zoom, availableHeight / height);

        graphState.setZoomLevel(zoom);

        float contentCenterX = (minCenterX + maxCenterX) / 2.0F;
        float contentCenterY = (minCenterY + maxCenterY) / 2.0F;
        float viewportCenterX = (VIEW_X1 + VIEW_X2) / 2.0F;
        float viewportCenterY = (VIEW_Y1 + VIEW_Y2) / 2.0F;
        graphState.setPan(
            viewportCenterX - contentCenterX * graphState.getZoomLevel(),
            viewportCenterY - contentCenterY * graphState.getZoomLevel());
        updateViewport();
    }

    private void generateMutationNodes(PositionedMutationNodeStack nodeStack) {
        int nodeX = nodeStack.getX();
        int nodeY = nodeStack.getY();

        if (nodeStack.leftChild == null || nodeStack.rightChild == null) {
            if (nodeStack.hasPossibleChildren) {
                Color drawColor = LINE_BLACK;

                if (nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                    drawColor = LINE_RED;
                }

                if (nodeStack.baseChance > 0) {
                    this.mutationNodesToRender.add(new ChanceInfoNode(nodeX, nodeY + 16, nodeStack));
                }

                this.mutationNodesToRender.add(
                    new LineNode(
                        nodeX + OFFSET_CORRECTION,
                        nodeY + OFFSET_CORRECTION,
                        nodeX + OFFSET_CORRECTION - 4,
                        nodeY + OFFSET_CORRECTION + POSSIBLE_CHILD_OFFSET,
                        drawColor));

                this.mutationNodesToRender.add(
                    new LineNode(
                        nodeX + OFFSET_CORRECTION,
                        nodeY + OFFSET_CORRECTION,
                        nodeX + OFFSET_CORRECTION + 4,
                        nodeY + OFFSET_CORRECTION + POSSIBLE_CHILD_OFFSET,
                        drawColor));
            }
        } else {
            PositionedMutationNodeStack left = nodeStack.leftChild;
            PositionedMutationNodeStack right = nodeStack.rightChild;

            Color drawColor = LINE_BLACK;

            if (nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                drawColor = LINE_RED;
            }

            if (nodeStack.baseChance > 0) {
                this.mutationNodesToRender.add(new ChanceInfoNode(nodeX, nodeY + 16, nodeStack));
            }

            this.mutationNodesToRender.add(
                new LineNode(
                    nodeX + OFFSET_CORRECTION,
                    nodeY + OFFSET_CORRECTION,
                    left.relx + OFFSET_CORRECTION,
                    left.rely + OFFSET_CORRECTION,
                    drawColor));

            this.mutationNodesToRender.add(
                new LineNode(
                    nodeX + OFFSET_CORRECTION,
                    nodeY + OFFSET_CORRECTION,
                    right.relx + OFFSET_CORRECTION,
                    right.rely + OFFSET_CORRECTION,
                    drawColor));

            generateMutationNodes(left);
            generateMutationNodes(right);
        }

    }

    private void removeAndReplaceDuplicates(SimpleBinaryTree.Node<IAllele> node, List<IAllele> discoveredMutations) { // Replace
                                                                                                                      // with
                                                                                                                      // 2
                                                                                                                      // lines.
        if (discoveredMutations.contains(node.getValue())) {
            List<IBeeMutation> mutations = getMutationsWithResult(node.getValue());
            if (!mutations.isEmpty()) node.removeDuplicate(); // Only if it actually has mutations that has this node as
                                                              // result.
        } else {
            discoveredMutations.add(node.getValue());
            if (node.getMaxFollowingDepth() > 0) {
                removeAndReplaceDuplicates(node.getLeftNode(), discoveredMutations);
                removeAndReplaceDuplicates(node.getRightNode(), discoveredMutations);
            }
        }
    }

    private boolean checkSeparationWidth(int maxTotalDepth, int xSeparationThreshold) {
        double maxDivision = Math.pow(2, maxTotalDepth + 1);
        int resultingLLWidth = (int) (120 / maxDivision); // LowestLevelWidth
        return resultingLLWidth < xSeparationThreshold;
    }

    private PositionedMutationNodeStack placeInRenderBuffer(SimpleBinaryTree.Node<IAllele> node, int minY, int yStep,
        int minX, int maxX, int iterationMaxCount) {
        int center = (minX + maxX) / 2;
        iterationMaxCount--;
        if (iterationMaxCount < 0 || node.getMaxFollowingDepth() <= 0) {
            List<IBeeMutation> mutationsToRoot = getMutationsWithResult(node.getValue());
            float ch = -1;
            Collection<String> requirements = new LinkedList<>();
            if (!mutationsToRoot.isEmpty()) {
                IBeeMutation mut = mutationsToRoot.get(0);
                ch = mut.getBaseChance();
                try {
                    requirements = mut.getSpecialConditions();
                } catch (Throwable tr) {
                    requirements = new LinkedList<>();
                }
            }
            PositionedMutationNodeStack leaf = new PositionedMutationNodeStack( // Leaf
                createStack((IAlleleSpecies) node.getValue(), BEE_TYPE_DRONE),
                center,
                minY,
                ch,
                requirements,
                null,
                null,
                node.getMaxFollowingDepth() > 0);
            evaluatedBeePositions.add(leaf);
            return leaf;
        }

        List<IBeeMutation> mutations = getMutationsWithResult(node.getValue());
        float ch = -1;
        Collection<String> requirements = new LinkedList<>();
        if (!mutations.isEmpty()) {
            IBeeMutation mut = mutations.get(0);
            ch = mut.getBaseChance();
            try {
                requirements = mut.getSpecialConditions();
            } catch (Throwable tr) {
                requirements = new LinkedList<>();
            }
        }

        PositionedMutationNodeStack outNode = new PositionedMutationNodeStack(
            createStack((IAlleleSpecies) node.getValue(), BEE_TYPE_DRONE),
            center,
            minY,
            ch,
            requirements,
            node.getLeftNode() != null
                ? placeInRenderBuffer(node.getLeftNode(), minY + yStep, yStep, minX, center, iterationMaxCount)
                : null,
            node.getRightNode() != null
                ? placeInRenderBuffer(node.getRightNode(), minY + yStep, yStep, center, maxX, iterationMaxCount)
                : null,
            true);
        evaluatedBeePositions.add(outNode);
        return outNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CachedBeeMutationTree that = (CachedBeeMutationTree) o;
        return Objects.equals(mutationTree, that.mutationTree);
    }

    @Override
    public int hashCode() {
        return mutationTree != null ? mutationTree.hashCode() : 0;
    }

    @Override
    public PositionedStack getResult() {
        return rootStack;
    }

    public PositionedMutationNodeStack getRootStack() {
        return rootStack;
    }

    public List<IMutationNode> getMutationNodesToRender() {
        return mutationNodesToRender;
    }

    public GraphInteractionState getGraphState() {
        return graphState;
    }

    @Override
    public List<PositionedStack> getIngredients() {
        return new ArrayList<>(evaluatedBeePositions);
    }

    public static class PositionedMutationNodeStack extends PositionedStack {

        public final int worldX;
        public final int worldY;

        public final boolean hasPossibleChildren;
        public final float baseChance;
        public final Collection<String> requirements;
        public final PositionedMutationNodeStack leftChild, rightChild;

        public PositionedMutationNodeStack(Object object, int x, int y, boolean genPerms, float baseChance,
            Collection<String> requirementInfo, PositionedMutationNodeStack leftChild,
            PositionedMutationNodeStack rightChild, boolean hasPossibleChildren) {
            super(object, x, y, genPerms);

            this.worldX = x;
            this.worldY = y;

            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.hasPossibleChildren = hasPossibleChildren;
            this.baseChance = baseChance;
            this.requirements = requirementInfo;
        }

        public PositionedMutationNodeStack(Object object, int x, int y, float baseChance,
            Collection<String> requirementInfo, PositionedMutationNodeStack leftChild,
            PositionedMutationNodeStack rightChild, boolean hasPossibleChildren) {
            super(object, x, y);

            this.worldX = x;
            this.worldY = y;

            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.hasPossibleChildren = hasPossibleChildren;
            this.baseChance = baseChance;
            this.requirements = requirementInfo;
        }

        public int getX() {
            return this.worldX;
        }

        public int getY() {
            return this.worldY;
        }

    }

}
