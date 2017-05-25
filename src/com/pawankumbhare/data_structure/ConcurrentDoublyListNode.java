package com.pawankumbhare.data_structure;

public class ConcurrentDoublyListNode<T> {

	public volatile T val;
	public volatile ConcurrentDoublyListNode<T> next, prev;

	public ConcurrentDoublyListNode(T x) {
		val = x;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("[");
		ConcurrentDoublyListNode<T> node = this;
		while (node != null) {
			String pre = node.next != null ? ", " : "";
			str.append(node.val.toString() + pre);
			node = node.next;
			//break;
		}
		str.append("]");
		return str.toString();
	}
}