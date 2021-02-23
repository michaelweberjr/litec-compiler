package common.util;

import java.util.ArrayList;

public class Node<S> {
	public ArrayList<Node<S>> children;
	public S val;
	
	public Node(S val)
	{
		this.val = val;
		children = new ArrayList<Node<S>>();
	}
	
	public void addChild(S val)
	{
		children.add(new Node<S>(val));
	}
	
	public void addChild(Node<S> child) throws Exception {
		if(child == null) throw new Exception("Child node must not be null!");
		children.add(child);
	}
	
	public int maxDepth() {
		if(children.size() == 0) return 1;
		
		int max = 0;
		for(Node<S> n : children) {
			int temp = n.maxDepth();
			if(temp > max) max = temp;
		}
		
		return max+1;
	}
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object e) {
		return ((Node<S>)e).val.equals(this.val);
	}
}
