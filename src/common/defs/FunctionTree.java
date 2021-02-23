package common.defs;

import java.util.ArrayList;

import common.util.Tree;

public class FunctionTree {
	public Tree<Token> statements;
	public Symbol name;
	public ArrayList<Symbol> argsList;
	public ArrayList<Symbol> frameVar;
	public Token returnType;
	public boolean hasReturn;
	public boolean isBuiltIn;
	public String builtInCode;
	
	public FunctionTree()
	{
		this.statements = new Tree<Token>(new Token(TokenType.ROOT));
		this.name = null;
		this.argsList = new ArrayList<Symbol>();
		this.frameVar = new ArrayList<Symbol>();
		this.hasReturn = false;
		this.isBuiltIn = false;
	}
}
