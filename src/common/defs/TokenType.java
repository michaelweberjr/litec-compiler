package common.defs;

public enum TokenType {
	ROOT, CALL,	// parser tree types
	NUM, SYM, // extra info types
	LPAR, RPAR, LBRACE, RBRACE, LBRACKET, RBRACKET, COMMA, SC, COLON, // single character types
	EQ, PLUS, MIN, MUL, DIV, MOD, // math types
	VOID, INT, RET, // keywords
	NL	// internal use
}
