package parser.util;

import java.util.ArrayList;

public class Node<S> {
	public ArrayList<Node<S>> children;
	public S val;
	
	public Node(S val)
	{
		this.val = val;
	}
	
	public void addChild(S val)
	{
		children.add(new Node<S>(val));
	}
	
	public void addChild(Node<S> child) {
		children.add(child);
	}
}
