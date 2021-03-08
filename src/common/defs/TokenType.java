package common.defs;

public enum TokenType {
	ROOT, CALL,	// parser tree types
	NUM, SYM, // extra info types
	LPAR, RPAR, LBRACE, RBRACE, LBRACKET, RBRACKET, COMMA, SC, COLON, // single character types
	ASGN, 
	EQ, NE, LESS, LE, GRT, GE, // comparisons 
	PLUS, MIN, MUL, DIV, MOD, // math types
	PP, MM, NEG,
	NOT, BNOT, OR, BOR, AND, BAND, BXOR, // boolean operations
	LSH, RSH,
	PLUSEQ, MINEQ, MULEQ, DIVEQ, MODEQ,
	ANDEQ, OREQ, XOREQ, SHLEQ, SHREQ,
	IF, ELSE, ELIF, FOR, WHILE, BREAK, CONT,
	VOID, INT, RET, // keywords
	NL	// internal use
}
