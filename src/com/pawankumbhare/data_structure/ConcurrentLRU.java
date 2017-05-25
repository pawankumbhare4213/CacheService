package com.pawankumbhare.data_structure;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentLRU<K, V> {	

	public class Node<Key, Value> {
		private volatile Key key;
		private volatile Value val;

		public Node(Key key, Value val) {
			this.key = key;
			this.val = val;
		}

		public Key getKey() {
			return key;
		}
		
		public Value getValue() {
			return val;
		}

		@Override
		public String toString() {
			return "Node [key=" + key + ", val=" + val + "]";
		}
	}

	public volatile int capacity = 0;
	private volatile int curr_capacity = 0;
	private volatile ConcurrentHashMap<K, ConcurrentDoublyListNode<Node<K, V>>> map;
	private volatile ConcurrentDoublyLinkedList<Node<K, V>> list;

	public ConcurrentLRU(int capacity) {
		this.capacity = capacity;
		this.map = new ConcurrentHashMap<>(capacity);
		this.list = new ConcurrentDoublyLinkedList<>();
	}
	
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public V get(K key) {
		if (map.containsKey(key)) {
			ConcurrentDoublyListNode<Node<K, V>> node = map.get(key);
			list.remove(node);
			list.addLast(node.val);
			return map.get(key).val.getValue();
		}
		return null;
	}

	public V put(K key, V val) {
		Node<K, V> oldNode = null;
		Node<K, V> newNode = new Node<>(key, val);
		if (map.containsKey(key)) {
			ConcurrentDoublyListNode<Node<K, V>> node = map.get(key);
			list.remove(node);
		} else if (this.capacity == curr_capacity) {
			oldNode = list.removeFirst();
			map.remove(oldNode.getKey());
		} else curr_capacity++;
		ConcurrentDoublyListNode<Node<K, V>> newListNode = list.addLast(newNode);
		map.put(key, newListNode);
		System.out.println("REMOVING FROM LRU : " + oldNode);
		return oldNode != null ? oldNode.val : null;
	}
	
	public boolean containsKey(K key) {
		return map.containsKey(key);
	}
	
	public V remove(K key) {
		V removedVal = null;
		if (map.containsKey(key)) {
			removedVal = map.get(key).val.val;
			list.remove(map.get(key));
			map.remove(key);
		}
		return removedVal;
	}

	@Override
	public String toString() {
		return "LRU [capacity=" + capacity + ", curr_capacity=" + curr_capacity + ", map=" + map + ", list=" + list + "]";
	}

	/*public static void main(String[] args) {
		LRU<Integer, Integer> cache = new LRU<>(2); // capacity
		cache.put(1, 1);
		cache.put(2, 2);
		System.out.println(cache.get(1)); // returns 1
		cache.put(3, 3); // evicts key 2
		System.out.println(cache.get(2)); // returns -1 (not found)
		cache.put(4, 4); // evicts key 1
		System.out.println(cache.get(1)); // returns -1 (not found)
		System.out.println(cache.get(3)); // returns 3
		System.out.println(cache.get(4)); // returns 4
	}*/
}
