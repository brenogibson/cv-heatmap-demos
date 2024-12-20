package awsPrototype.helpers;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentLRUCache<K, V> {

    private final int capacity;
    private final ConcurrentHashMap<K, Node<K, V>> map;
    private final Node<K, V> head;
    private final Node<K, V> tail;

    public ConcurrentLRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new ConcurrentHashMap<>();
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) {
            return null;
        }
        synchronized (this) {
            moveToHead(node);
        }
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> node = map.get(key);
        if (node == null) {
            node = new Node<>(key, value);
            map.put(key, node);
            synchronized (this) {
                addNode(node);
            }
            if (map.size() > capacity) {
                synchronized (this) {
                    removeTail();
                }
            }
        } else {
            node.value = value;
            synchronized (this) {
                moveToHead(node);
            }
        }
    }

    public void remove(K key) {
        Node<K, V> node = map.get(key);
        if (node != null) {
            synchronized (this) {
                removeNode(node);
                map.remove(key);
            }
        }
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        synchronized (this) {
            map.clear();
            head.next = tail;
            tail.prev = head;
        }
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    private void addNode(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addNode(node);
    }

    private void removeTail() {
        Node<K, V> tailNode = tail.prev;
        if (tailNode != head) {
            removeNode(tailNode);
            map.remove(tailNode.key);
        }
    }

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

}
