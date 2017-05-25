package com.pawankumbhare.data_structure;

public class ConcurrentDoublyLinkedList<T> {
	public volatile ConcurrentDoublyListNode<T> start, end;

	public void remove(ConcurrentDoublyListNode<T> node) {
		if (node == null) return;
		if (node.prev == null) removeFirst();
		else node.prev.next = node.next;
		if (node.next == null) removeLast();
		else node.next.prev = node.prev;
	}

	public T removeLast() {
		if (end == null) return null;
		ConcurrentDoublyListNode<T> removed = end;
		if (end.prev == null) {
			start = null;
			end = null;
			return removed.val;
		}
		end = end.prev;
		end.next = null;
		return removed.val;
	}
	
	public ConcurrentDoublyListNode<T> removeLastWithGetNode() {
		if (end == null) return null;
		ConcurrentDoublyListNode<T> removed = end;
		if (end.prev == null) {
			start = null;
			end = null;
			return removed;
		}
		end = end.prev;
		end.next = null;
		return removed;
	}

	public T removeFirst() {
		if (start == null) return null;
		ConcurrentDoublyListNode<T> removed = start;
		if (start.next == null) {
			start = null;
			end = null;
			return removed.val;
		}
		start = start.next;
		start.prev = null;
		return removed.val;
	}
	
	public ConcurrentDoublyListNode<T> removeFirstWithGetNode() {
		if (start == null) return null;
		ConcurrentDoublyListNode<T> removed = start;
		if (start.next == null) {
			start = null;
			end = null;
			return removed;
		}
		start = start.next;
		start.prev = null;
		return removed;
	}

	public ConcurrentDoublyListNode<T> addLast(T node) {
		ConcurrentDoublyListNode<T> newNode = new ConcurrentDoublyListNode<>(node);
		if(start == null || end == null) {
			start = newNode;
			end = newNode;
			return end;
		}
		end.next = newNode;
		newNode.prev = end;
		end = end.next;
		return end;
	}
	
	public ConcurrentDoublyListNode<T> addLast(ConcurrentDoublyListNode<T> newNode) {
		if(start == null || end == null) {
			start = newNode;
			end = newNode;
			return end;
		}
		end.next = newNode;
		newNode.prev = end;
		end = end.next;
		return end;
	}
	
	public ConcurrentDoublyListNode<T> addFirst(T node) {
		ConcurrentDoublyListNode<T> newNode = new ConcurrentDoublyListNode<>(node);
		if(start == null || end == null) {
			start = newNode;
			end = newNode;
			return start;
		}
		newNode.next = start;
		start.prev = newNode;
		start = newNode;
		return start;
	}
	
	public ConcurrentDoublyListNode<T> addFirst(ConcurrentDoublyListNode<T> newNode) {
		if(start == null || end == null) {
			start = newNode;
			end = newNode;
			return start;
		}
		newNode.next = start;
		start.prev = newNode;
		start = newNode;
		return start;
	}

	@Override
	public String toString() {
		return "S[" + start + "]";//"ConcurrentDoublyLinkedList [start=" + start + "]";//+ ", end=" + end + "]";
	}
}
