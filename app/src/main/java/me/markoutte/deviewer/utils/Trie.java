/*
 * Copyright 2025 Maksim Pelevin and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.markoutte.deviewer.utils;

import java.util.*;
import java.util.stream.Collectors;

public class Trie<T, K> implements Iterable<List<T>> {

    private final Map<K, NodeImpl<T, K>> roots = new HashMap<>();
    private final Map<Node<T>, NodeImpl<T, K>> implementations = new HashMap<>();
    private final KeyExtractor<T, K> keyExtractor;

    public Trie(KeyExtractor<T, K> keyExtractor) {
        this.keyExtractor = keyExtractor;
    }

    /**
     * Adds value into a trie.
     *
     * If value already exists then do nothing except increasing internal counter of added values.
     * The counter can be returned by Node.count.
     *
     * @return corresponding Node of the last element in the `values`
     */
    public Node<T> add(Iterable<T> values) {
        Iterator<T> iterator = values.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("Empty list are not allowed");
        }
        T root = iterator.next();
        K key = keyExtractor.extractKey(root);
        NodeImpl<T, K> node = roots.computeIfAbsent(key, k -> new NodeImpl<>(root, null));
        node.hit++;
        while (iterator.hasNext()) {
            T value = iterator.next();
            key = keyExtractor.extractKey(value);
            final var fNode = node;
            node = node.children.computeIfAbsent(key, k -> new NodeImpl<>(value, fNode));
            node.hit++;
        }
        node.count++;
        implementations.put(node, node);
        return node;
    }

    /**
     * Decreases node counter value or removes the value completely if `counter == 1`.
     *
     * Use removeCompletely to remove the value from the trie regardless of counter value.
     *
     * @return removed node if value exists.
     */
    public Node<T> remove(Iterable<T> values) {
        NodeImpl<T, K> node = findImpl(values, false);
        if (node == null) return null;

        if (node.count == 1) {
            return removeCompletely(values);
        } else if (node.count > 1) {
            node.count--;
            return node;
        } else {
            throw new IllegalStateException("count should be 1 or greater");
        }
    }

    /**
     * Removes value from a trie.
     *
     * The value is removed completely from the trie. Thus, the next code is true:
     *
     * ```
     * trie.remove(someValue)
     * trie.get(someValue) == null
     * ```
     *
     * Use remove to decrease counter value instead of removal.
     *
     * @return removed node if value exists
     */
    public Node<T> removeCompletely(Iterable<T> values) {
        NodeImpl<T, K> node = findImpl(values, false);
        if (node == null) return null;

        if (node.count > 0 && node.children.isEmpty()) {
            NodeImpl<T, K> n = node;
            while (n != null) {
                K key = keyExtractor.extractKey(n.data);
                n = n.parent;
                if (n == null) {
                    NodeImpl<T, K> removed = roots.remove(key);
                    Objects.requireNonNull(removed);
                } else {
                    NodeImpl<T, K> removed = n.children.remove(key);
                    Objects.requireNonNull(removed);
                    if (n.count != 0) {
                        break;
                    }
                }
            }
        }

        if (node.count > 0) {
            node.count = 0;
            implementations.remove(node);
            return node;
        } else {
            return null;
        }
    }

    public Node<T> get(Iterable<T> values) {
        return findImpl(values, false);
    }

    public Node<T> getImpl(Iterable<T> values) {
        return findImpl(values, true);
    }

    public List<T> get(Node<T> node) {
        NodeImpl<T, K> implNode = implementations.get(node);
        return implNode != null ? buildValue(implNode) : null;
    }

    public List<Node<T>> children(Node<T> parent) {
        return ((NodeImpl<T, K>) parent).children.values()
                .stream()
                .sorted(Comparator.comparingInt(value -> -value.hit))
                .collect(Collectors.toUnmodifiableList());
    }

    private NodeImpl<T, K> findImpl(Iterable<T> values, boolean raw) {
        Iterator<T> iterator = values.iterator();
        if (!iterator.hasNext()) return null;

        T root = iterator.next();
        K key = keyExtractor.extractKey(root);
        NodeImpl<T, K> node = roots.get(key);
        if (node == null) return null;

        while (iterator.hasNext()) {
            T value = iterator.next();
            key = keyExtractor.extractKey(value);
            node = node.children.get(key);
            if (node == null) return null;
        }

        return raw || node.count > 0 ? node : null;
    }

    @Override
    public Iterator<List<T>> iterator() {
        List<List<T>> resultList = new ArrayList<>();
        for (NodeImpl<T, K> root : roots.values()) {
            traverseImpl(root, resultList);
        }
        return resultList.iterator();
    }

    private void traverseImpl(NodeImpl<T, K> node, List<List<T>> resultList) {
        Deque<NodeImpl<T, K>> stack = new ArrayDeque<>();
        stack.addLast(node);

        while (!stack.isEmpty()) {
            NodeImpl<T, K> n = stack.removeLast();
            if (n.count > 0) {
                resultList.add(buildValue(n));
            }
            stack.addAll(n.children.values());
        }
    }

    private List<T> buildValue(NodeImpl<T, K> node) {
        List<T> resultList = new ArrayList<>();
        while (node != null) {
            resultList.add(node.data);
            node = node.parent;
        }
        Collections.reverse(resultList);
        return resultList;
    }

    public interface Node<U> {
        U getData();

        int getCount();

        int getHit();
    }

    private static class NodeImpl<U, V> implements Node<U> {

        private final U data;
        private final NodeImpl<U, V> parent;
        private int count;
        private final Map<V, NodeImpl<U, V>> children;
        private int hit;

        public NodeImpl(U data, NodeImpl<U, V> parent) {
            this.data = data;
            this.parent = parent;
            this.count = 0;
            this.children = new HashMap<>();
            this.hit = 0;
        }

        @Override
        public U getData() {
            return data;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public int getHit() {
            return hit;
        }
    }

    private static class EmptyNode implements Node<Object> {

        @Override
        public Object getData() {
            throw new UnsupportedOperationException("empty node has no data");
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public int getHit() {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    public static <U> Node<U> emptyNode() {
        return (Node<U>) new EmptyNode();
    }

    @FunctionalInterface
    public interface KeyExtractor<U, V> {
        V extractKey(U input);
    }
}

