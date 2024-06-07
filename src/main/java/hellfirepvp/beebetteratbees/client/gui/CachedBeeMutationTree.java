package hellfirepvp.beebetteratbees.client.gui;

import static hellfirepvp.beebetteratbees.client.gui.BBABGuiRecipeTreeHandler.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import codechicken.nei.PositionedStack;
import forestry.api.apiculture.IBeeMutation;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import hellfirepvp.beebetteratbees.client.util.SimpleBinaryTree;
import hellfirepvp.beebetteratbees.common.ModConfig;

/**
 * HellFirePvP@Admin
 * Date: 28.04.2016 / 23:44
 * on BeeBetterAtBees
 * CachedBeeMutationTree
 */
public class CachedBeeMutationTree extends CachedRecipe {

    // RENDERING BB
    // X = 15 to 135 (Size: 120)
    // Y = 10 to 100 (Size: 90)

    private static final int MIN_X = 15, MAX_X = 135;
    private static final int X_SEPERATION_THRESHOLD = 7;
    private static final int Y_OFFSET = 0;

    private final SimpleBinaryTree<IAllele> mutationTree;
    private final List<PositionedMutationNodeStack> evaluatedBeePositions;
    private int evaluatedMaxX;
    public final boolean oversized;
    private PositionedMutationNodeStack rootStack;

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

    @Override
    public List<PositionedStack> getIngredients() {
        return new ArrayList<>(evaluatedBeePositions);
    }

    public static class PositionedMutationNodeStack extends PositionedStack {

        public final boolean hasPossibleChildren;
        public final float baseChance;
        public final Collection<String> requirements;
        public final PositionedMutationNodeStack leftChild, rightChild;

        public PositionedMutationNodeStack(Object object, int x, int y, boolean genPerms, float baseChance,
            Collection<String> requirementInfo, PositionedMutationNodeStack leftChild,
            PositionedMutationNodeStack rightChild, boolean hasPossibleChildren) {
            super(object, x, y, genPerms);
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
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.hasPossibleChildren = hasPossibleChildren;
            this.baseChance = baseChance;
            this.requirements = requirementInfo;
        }

    }

}
