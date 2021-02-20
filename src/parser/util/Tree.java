package parser.util;

public class Tree<S> {
	public Node<S> root;
	
	public Tree(S root_val)
	{
		this.root = new Node<S>(root_val);
	}
	
	public Tree()
	{
		this.root = null;
	}
	
	public int maxDepth() {
		return root.maxDepth();
	}
}
