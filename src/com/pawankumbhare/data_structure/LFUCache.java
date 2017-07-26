package com.pawankumbhare.data_structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class LFUCache<K, V> {

    private Node 				head;
    private int 				capacity;
    private HashMap<K, V> 		valueMap;
    private HashMap<K, Node>	nodeMap;
    
    public LFUCache(int capacity) {
        this.capacity 	= capacity;
        valueMap 		= new HashMap<>();
        nodeMap 		= new HashMap<>();
    }
    
    public boolean containsKey(K key) {
    	boolean out = valueMap.containsKey(key);
    	if (out) increaseFreq(key);
    	return out;
    }
    
    public V get(K key) {
        if (valueMap.containsKey(key)) {
        	increaseFreq(key);
            return valueMap.get(key);
        }
        return null;
    }
    
    public V set(K key, V value) {
        if (capacity == 0) return null;
        V oldVal = null;
        if (valueMap.containsKey(key)) valueMap.put(key, value);
        else {
            if (valueMap.size() < capacity) valueMap.put(key, value);
            else {
            	oldVal = removeOld();
                valueMap.put(key, value);
            }
            addInFront(key);
        }
        increaseFreq(key);
        return oldVal;
    }
    
    private void addInFront(K key) {
        if (head == null) {
            head = new Node(0);
            head.keys.add(key);
        } else if (head.freq > 0) {
            Node node = new Node(0);
            node.keys.add(key);
            node.next = head;
            head.prev = node;
            head = node;
        } else {
            head.keys.add(key);
        }
        nodeMap.put(key, head);      
    }
    
    private void increaseFreq(K key) {
        Node node = nodeMap.get(key);
        if (node == null) return;
        node.keys.remove(key);
        if (node.next == null) {
            node.next = new Node(node.freq + 1);
            node.next.prev = node;
            node.next.keys.add(key);
        } else if (node.next.freq == node.freq + 1) node.next.keys.add(key);
        else {
            Node tmp = new Node(node.freq + 1);
            tmp.keys.add(key);
            tmp.prev = node;
            tmp.next = node.next;
            node.next.prev = tmp;
            node.next = tmp;
        }
        nodeMap.put(key, node.next);
        if (node.keys.size() == 0) remove(node);
    }
    
    private V removeOld() {
        if (head == null) return null;
        K oldKey = null;
        Iterator<K> itr = head.keys.iterator();
        if (itr.hasNext()) oldKey = itr.next();
        V oldVal = valueMap.get(oldKey);
        head.keys.remove(oldKey);
        if (head.keys.size() == 0) remove(head);
        nodeMap.remove(oldKey);
        valueMap.remove(oldKey);
        return oldVal;
    }
    
    private void remove(Node node) {
        if (node.prev == null) head = node.next;
        else node.prev.next = node.next;
        if (node.next != null) node.next.prev = node.prev;
    }
    
    private class Node {
        public int freq = 0;
        public LinkedHashSet<K> keys = null;
        public Node prev = null, next = null;
        
        public Node(int freq) {
            this.freq = freq;
            keys = new LinkedHashSet<>();
            prev = next = null;
        }

		@Override
		public String toString() {
			return "Node [freq=" + freq + ", keys=" + keys + "]";
		}
    }

	public void remove(K key) {
		if (!nodeMap.containsKey(key)) return;
		remove(nodeMap.get(key));
		valueMap.remove(key);
	}

	@Override
	public String toString() {
		return "LFUCache [head=" + head + ", capacity=" + capacity + ", valueMap=" + valueMap + ", nodeMap=" + nodeMap + "]";
	}
}
