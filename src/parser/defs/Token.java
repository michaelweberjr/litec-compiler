package parser.defs;

public class Token {
	public TokenType type;
	public Symbol sym;
	public int val;
	
	public Token(TokenType type)
	{
		this(type, 0);
	}
	
	public Token(TokenType type, int val)
	{
		this.type = type;
		this.val = val;
	}
	
	public Token(TokenType type, Symbol sym)
	{
		this.type = type;
		this.sym = sym;
	}
	
	public boolean opEquals(Object e) {
		return ((Token) e).type == this.type;
	}
}
