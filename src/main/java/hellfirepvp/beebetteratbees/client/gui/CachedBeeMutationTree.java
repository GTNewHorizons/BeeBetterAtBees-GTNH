package hellfirepvp.beebetteratbees.client.gui;

import static hellfirepvp.beebetteratbees.client.gui.BBABGuiRecipeTreeHandler.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import forestry.api.apiculture.IBeeMutation;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import hellfirepvp.beebetteratbees.client.gui.graph.GraphInteractionState;
import hellfirepvp.beebetteratbees.client.requirements.BlockRequirement;
import hellfirepvp.beebetteratbees.client.requirements.RequirementLayout;
import hellfirepvp.beebetteratbees.client.requirements.RequirementResolvers;
import hellfirepvp.beebetteratbees.client.requirements.RequirementSlot;
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

    public interface IMutationNode {

        boolean containsPoint(int x, int y, GraphInteractionState state);

        void renderNode(GraphInteractionState state);
    }

    public static class LineNode implements IMutationNode {

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

        @Override
        public void renderNode(GraphInteractionState state) {
            if (state == null) return;

            GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glLineWidth(3.0F);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, 0.5F);

            float[] start = state.worldToScreen((float) lx, (float) ly);
            float[] end = state.worldToScreen((float) hx, (float) hy);

            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex3f(start[0], start[1], 0);
            GL11.glVertex3f(end[0], end[1], 0);
            GL11.glEnd();
            GL11.glPopAttrib();
        }
    }

    public static class ChanceInfoNode implements IMutationNode {

        public final int x;
        public final int y;
        public final float chance;
        public Color drawColor = LINE_BLACK;
        public final String displayString;
        public Collection<String> infoLines;

        @Override
        public void renderNode(GraphInteractionState state) {
            if (state == null) return;
            float[] drawPosition = state.worldToScreen(this.x + 8, this.y + 1);
            GL11.glPushMatrix();
            GL11.glTranslatef(drawPosition[0], drawPosition[1], 0);
            GL11.glScalef(0.65F, 0.65F, 0.65F);
            GuiDraw.drawStringC(this.displayString, 0, 0, drawColor.getRGB(), false);
            GL11.glPopMatrix();
        }

        public ChanceInfoNode(int x, int y, PositionedMutationNodeStack nodeStack) {
            this.x = x;
            this.y = y;
            this.chance = nodeStack.baseChance;

            if (nodeStack.requirements != null && !nodeStack.requirements.isEmpty()) {
                this.drawColor = LINE_RED;
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
    }

    private static final int MIN_X = 2, MAX_X = 162;
    private static final int X_SEPERATION_THRESHOLD = 7;
    private static final int Y_OFFSET = 0;
    private static final Color LINE_BLACK = new Color(ColorUtils.neiLineBlack.getColor(), true);
    private static final Color LINE_RED = new Color(ColorUtils.neiLineRed.getColor(), true);

    private static final int OFFSET_CORRECTION = 8;
    private static final int POSSIBLE_CHILD_OFFSET = 16;
    private static final int LEVEL_STEP = 50;

    // for scissors
    private static final int VIEW_X1 = 15;
    private static final int VIEW_Y1 = 0;
    private static final int VIEW_X2 = 135;
    private static final int VIEW_Y2 = 100;

    private final SimpleBinaryTree<IAllele> mutationTree;
    private final int treeId;
    private static int GLOBAL_ID = 0;
    private final GraphInteractionState graphState = new GraphInteractionState();
    private final List<PositionedMutationNodeStack> evaluatedBeePositions;
    private int evaluatedMaxX;
    public final boolean oversized;
    private PositionedMutationNodeStack rootStack;
    private List<IMutationNode> mutationNodesToRender = new LinkedList<>();
    private final List<RequirementSlot> requirementSlots = new ArrayList<>();
    private final List<float[]> requirementSlotWorldPositions = new ArrayList<>();

    public void updateViewport() {
        if (rootStack == null) return;
        updateStackPosition(rootStack);

        for (PositionedMutationNodeStack stack : evaluatedBeePositions) {
            updateStackPosition(stack);
        }
        for (int i = 0; i < requirementSlots.size(); i++) {
            float[] world = requirementSlotWorldPositions.get(i);
            float[] screen = graphState.worldToScreen(world[0], world[1]);
            requirementSlots.get(i).relx = Math.round(screen[0]);
            requirementSlots.get(i).rely = Math.round(screen[1]);
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
        this.treeId = GLOBAL_ID++;

        if (!ModConfig.showDuplicateTrees) {
            List<IAllele> foundMutationTrees = new ArrayList<>();
            removeAndReplaceDuplicates(this.mutationTree.getRoot(), foundMutationTrees);
        }

        // Important: We don't need to buffer root, because that's the "result"
        this.evaluatedBeePositions = new LinkedList<>();
        this.mutationNodesToRender = new LinkedList<>();
        // get max amount
        int iterationDepth = mutationTree.getRoot()
            .getMaxFollowingDepth();
        int maxTotalDepth = Math.min(
            iterationDepth,
            mutationTree.getRoot()
                .getMaxFollowingDepth());
        if (maxTotalDepth <= 0) {
            oversized = false;
            return; // In case the root is a leaf, there is nothing to display anyway except the root.
        }
        int yStep = LEVEL_STEP;

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

        float ch = parentMutation.getBaseChance();
        Collection<String> requirements = getSpecialConditions(parentMutation);

        this.rootStack = new PositionedMutationNodeStack(
            createStack(
                (IAlleleSpecies) mutationTree.getRoot()
                    .getValue(),
                BEE_TYPE_PRINCESS),
            (IAlleleSpecies) mutationTree.getRoot()
                .getValue(),
            (MIN_X + this.evaluatedMaxX) / 2 - OFFSET_CORRECTION,
            Y_OFFSET,
            ch,
            requirements,
            leftChild,
            rightChild,
            true);

        generateMutationNodes(rootStack);
        buildRequirementSlots(parentMutation);
        frameInitialGeneration();
    }

    private void buildRequirementSlots(IBeeMutation parentMutation) {
        addRequirements(rootStack, RequirementResolvers.resolve(parentMutation));
        List<PositionedMutationNodeStack> orderedStacks = new ArrayList<>(evaluatedBeePositions);
        orderedStacks.sort(
            Comparator.comparingInt((PositionedMutationNodeStack stack) -> stack.rely)
                .thenComparingInt(stack -> stack.relx));
        for (PositionedMutationNodeStack stack : orderedStacks) {
            addRequirements(stack, resolveRequirements(stack.species));
        }
    }

    private void addRequirements(PositionedMutationNodeStack stack, List<BlockRequirement> requirements) {
        if (stack == null || requirements.isEmpty()) return;
        List<RequirementSlot> placed = RequirementLayout.place(requirements, stack.relx, stack.rely);
        for (RequirementSlot slot : placed) {
            // still world coords at this point, updateViewport moves them with the bees later
            requirementSlotWorldPositions.add(new float[] { slot.relx, slot.rely });
        }
        requirementSlots.addAll(placed);
    }

    private List<BlockRequirement> resolveRequirements(IAllele species) {
        List<IBeeMutation> mutations = getMutationsWithResult(species);
        return mutations.isEmpty() ? Collections.emptyList() : RequirementResolvers.resolve(mutations.get(0));
    }

    private static Collection<String> getSpecialConditions(IBeeMutation mutation) {
        try {
            Collection<String> conditions = mutation.getSpecialConditions();
            return conditions == null ? Collections.<String>emptyList() : conditions;
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
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

        if (nodeStack.leftChild == null && nodeStack.rightChild == null) {
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
        int leafCount = 1 << maxTotalDepth;
        int resultingLLWidth = (MAX_X - MIN_X) / leafCount;
        return resultingLLWidth < xSeparationThreshold;
    }

    private PositionedMutationNodeStack placeInRenderBuffer(SimpleBinaryTree.Node<IAllele> node, int minY, int yStep,
        int minX, int maxX, int iterationMaxCount) {
        int center = (minX + maxX) / 2;
        iterationMaxCount--;
        if (iterationMaxCount < 0 || node.getMaxFollowingDepth() <= 0) {
            List<IBeeMutation> mutationsToRoot = getMutationsWithResult(node.getValue());
            float ch = -1;
            Collection<String> requirements = Collections.emptyList();
            if (!mutationsToRoot.isEmpty()) {
                ch = mutationsToRoot.get(0)
                    .getBaseChance();
                requirements = getSpecialConditions(mutationsToRoot.get(0));
            }
            PositionedMutationNodeStack leaf = new PositionedMutationNodeStack( // Leaf
                createStack((IAlleleSpecies) node.getValue(), BEE_TYPE_DRONE),
                (IAlleleSpecies) node.getValue(),
                center - OFFSET_CORRECTION,
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
        Collection<String> requirements = Collections.emptyList();
        if (!mutations.isEmpty()) {
            ch = mutations.get(0)
                .getBaseChance();
            requirements = getSpecialConditions(mutations.get(0));
        }

        PositionedMutationNodeStack outNode = new PositionedMutationNodeStack(
            createStack((IAlleleSpecies) node.getValue(), BEE_TYPE_DRONE),
            (IAlleleSpecies) node.getValue(),
            center - OFFSET_CORRECTION,
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
        if (!(o instanceof CachedBeeMutationTree)) return false;
        return this.treeId == ((CachedBeeMutationTree) o).treeId;
    }

    @Override
    public int hashCode() {
        return treeId;
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

    public List<RequirementSlot> getRequirementSlots() {
        return requirementSlots;
    }

    public int getRecipeHeight() {
        int maxY = RequirementLayout.MIN_RECIPE_HEIGHT;
        for (RequirementSlot slot : requirementSlots) {
            maxY = Math.max(maxY, slot.rely + RequirementLayout.SLOT_SIZE + 4);
        }
        for (PositionedMutationNodeStack stack : evaluatedBeePositions) maxY = Math.max(maxY, stack.rely + 24);
        return maxY;
    }

    public GraphInteractionState getGraphState() {
        return graphState;
    }

    @Override
    public List<PositionedStack> getIngredients() {
        List<PositionedStack> result = new ArrayList<>(evaluatedBeePositions);
        result.addAll(requirementSlots);
        return result;
    }

    public static class PositionedMutationNodeStack extends PositionedStack {

        public final int worldX;
        public final int worldY;

        public final boolean hasPossibleChildren;
        public final float baseChance;
        public final Collection<String> requirements;
        public final IAlleleSpecies species;
        public final PositionedMutationNodeStack leftChild, rightChild;

        public PositionedMutationNodeStack(Object object, IAlleleSpecies species, int x, int y, boolean genPerms,
            float baseChance, Collection<String> requirementInfo, PositionedMutationNodeStack leftChild,
            PositionedMutationNodeStack rightChild, boolean hasPossibleChildren) {
            super(object, x, y, genPerms);

            this.worldX = x;
            this.worldY = y;

            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.hasPossibleChildren = hasPossibleChildren;
            this.baseChance = baseChance;
            this.requirements = requirementInfo;
            this.species = species;
        }

        public PositionedMutationNodeStack(Object object, IAlleleSpecies species, int x, int y, float baseChance,
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
            this.species = species;
        }

        public int getX() {
            return this.worldX;
        }

        public int getY() {
            return this.worldY;
        }
    }

}
