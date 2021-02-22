package parser.defs;

public enum TokenType {
	ROOT, CALL,	// parser tree types
	NUM, SYM, // extra info types
	LPAR, RPAR, LBRA, RBRA, COMMA, SC, // single character types
	EQ, PLUS, MIN, MUL, DIV, MOD, // math types
	VOID, INT, RET, // keywords
	NL	// internal use
}
