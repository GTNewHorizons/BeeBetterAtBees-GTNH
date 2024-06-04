package hellfirepvp.beebetteratbees.client.util;

import java.util.Objects;

/**
 * HellFirePvP@Admin
 * Date: 29.04.2016 / 00:23
 * on BeeBetterAtBees
 * SimpleBinaryTree
 */
public class SimpleBinaryTree<E> {

    private final Node<E> root;
    private final RootProvider<E> rootProvider;

    public SimpleBinaryTree(int maxDepth, E rootValue, RootProvider<E> provider) {
        this.rootProvider = provider;
        this.root = buildSubTree(rootValue, maxDepth);
    }

    public SimpleBinaryTree(E rootValue, RootProvider<E> provider) {
        this(Integer.MAX_VALUE, rootValue, provider);
    }

    public Node<E> getRoot() {
        return root;
    }

    private Node<E> buildSubTree(E superNodeValue, int maxFollowingDepth) {
        E[] subNodes = rootProvider.provideSubNodes(superNodeValue);
        if (subNodes == null || subNodes.length == 0 || maxFollowingDepth <= 0) {
            return new Leaf<>(superNodeValue);
        } else {
            Node<E> left = buildSubTree(subNodes[0], maxFollowingDepth - 1);
            Node<E> right = buildSubTree(subNodes[1], maxFollowingDepth - 1);
            int depth = Math.max(left.getMaxFollowingDepth(), right.getMaxFollowingDepth());
            return new Node<>(superNodeValue, left, right, depth + 1);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleBinaryTree<?> that = (SimpleBinaryTree<?>) o;
        return Objects.equals(root, that.root);
    }

    @Override
    public int hashCode() {
        return root != null ? root.hashCode() : 0;
    }

    public abstract static class RootProvider<E> {

        public abstract E[] provideSubNodes(E superNode);

    }

    public static class Node<E> {

        protected final E value;
        private int maxFollowingDepth;
        private Node<E> left, right;

        private Node(E value, Node<E> left, Node<E> right, int depth) {
            this.value = value;
            this.left = left;
            this.right = right;
            this.maxFollowingDepth = depth;
        }

        public int getMaxFollowingDepth() {
            return maxFollowingDepth;
        }

        public E getValue() {
            return value;
        }

        public Node<E> getLeftNode() {
            return left;
        }

        public Node<E> getRightNode() {
            return right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node<?> node = (Node<?>) o;
            return Objects.equals(value, node.value);
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("[")
                .append(left.toString())
                .append("], ")
                .append(value.toString())
                .append(", [")
                .append(right.toString())
                .append("]")
                .toString();
        }

        public void removeDuplicate() {
            this.left = null;
            this.right = null;
            this.maxFollowingDepth = 1;
        }

    }

    private static class Leaf<E> extends Node<E> {

        private Leaf(E value) {
            super(value, null, null, 0);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

}
