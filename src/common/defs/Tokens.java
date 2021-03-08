package common.defs;

import java.util.Deque;
import java.util.LinkedList;

import parser.ParserError;

public class Tokens {
	public TokenType type;
	public Symbol sym;
	public int val;
	
	private static Deque<Tokens> queue = new LinkedList<Tokens>();
	
	public Tokens(TokenType type)
	{
		this(type, 0);
	}
	
	public Tokens(TokenType type, int val)
	{
		this.type = type;
		this.val = val;
	}
	
	public Tokens(TokenType type, Symbol sym)
	{
		this.type = type;
		this.sym = sym;
	}
	
	@Override
	public boolean equals(Object e) {
		return ((Tokens) e).type == this.type;
	}
	
	public static Tokens popFront() {
		while(queue.peek().type == TokenType.NL) {
			queue.poll();
			ParserError.lineNum++;
		}
		return queue.poll();
	}
	
	public static Tokens peekFront() {
		while(queue.peek().type == TokenType.NL) {
			queue.poll();
			ParserError.lineNum++;
		}
		return queue.peek();
	}
	
	public static void pushFront(Tokens t) {
		queue.addFirst(t);
	}
	
	public static void pushBack(Tokens t) {
		queue.addLast(t);
	}
	
	public static boolean isEmpty() {
		return queue.isEmpty();
	}
	
	public static boolean isMathToken(TokenType type) {
		switch(type) {
		case NEG:
		case PP:
		case MM:
		case NOT:
		case BNOT:
		case MUL:
		case DIV:
		case MOD:
		case PLUS:
		case MIN:
		case LSH:
		case RSH:
		case LESS:
		case LE:
		case GRT:
		case GE:
		case EQ:
		case NE:
		case BAND:
		case BXOR:
		case BOR:
		case AND:
		case OR:
			return true;
		default:
			return false;
		}
	}
	
	public static int evaluationLevel(TokenType type) throws Exception {
		switch(type) {
		case LPAR:
			return 0;
		case CALL:
			return 12;
		case NEG:
		case PP:
		case MM:
		case NOT:
		case BNOT:
			return 11;
		case MUL:
		case DIV:
		case MOD:
			return 10;
		case PLUS:
		case MIN:
			return 9;
		case LSH:
		case RSH:
			return 8;
		case LESS:
		case GRT:
			return 7;
		case EQ:
		case NE:
			return 6;
		case BAND:
			return 5;
		case BXOR:
			return 4;
		case BOR:
			return 3;
		case AND:
			return 2;
		case OR:
			return 1;
		default:
			return -1;
		}
	}
	
	public static int mathArgCount(TokenType type) {
		switch(type) {
		case NEG:
		case PP:
		case MM:
		case NOT:
		case BNOT:
			return 1;
		case MUL:
		case DIV:
		case MOD:
		case PLUS:
		case MIN:
		case LSH:
		case RSH:
		case LESS:
		case GRT:
		case EQ:
		case NE:
		case BAND:
		case BXOR:
		case BOR:
		case AND:
		case OR:
			return 2;
		default:
			return -1;
		}
	}
	
	public static boolean isCmpToken(TokenType type) {
		switch(type) {
		case EQ:
		case NE:
		case LESS:
		case LE:
		case GRT:
		case GE:
			return true;
		default:
			return false;
		}
	}
	
	public static boolean isBoolToken(TokenType type) {
		switch(type) {
		case NOT:
		case AND:
		case OR:
			return true;
		default:
			return false;
		}
	}
	
	public static TokenType reverseCmp(TokenType type) {
		switch(type) {
		case EQ:
			return TokenType.NE;
		case NE:
			return TokenType.EQ;
		case LESS:
			return TokenType.GE;
		case LE:
			return TokenType.GRT;
		case GRT:
			return TokenType.LE;
		case GE:
			return TokenType.LESS;
		default:
			return type;
		}
	}
	
	public static boolean isIncDec(TokenType type) {
		if(type == TokenType.PP || type == TokenType.MM) return true;
		return false;
	}
	
	public static boolean isOpEq(TokenType type) {
		switch(type) {
		case ASGN:
		case PLUSEQ:
		case MINEQ:
		case MULEQ:
		case DIVEQ:
		case MODEQ:
		case ANDEQ:
		case OREQ:
		case XOREQ:
		case SHLEQ:
		case SHREQ:
			return true;
		default:
			return false;
		}
	}
}
