package common.defs;

import java.util.ArrayList;

import common.util.Tree;

public class FunctionTree {
	public Tree<Tokens> statements;
	public Symbol name;
	public ArrayList<Symbol> argsList;
	public ArrayList<Symbol> frameVar;
	public Tokens returnType;
	public boolean hasReturn;
	public boolean isBuiltIn;
	public String builtInCode;
	
	public FunctionTree()
	{
		this.statements = new Tree<Tokens>(new Tokens(TokenType.ROOT));
		this.name = null;
		this.argsList = new ArrayList<Symbol>();
		this.frameVar = new ArrayList<Symbol>();
		this.hasReturn = false;
		this.isBuiltIn = false;
	}
}
