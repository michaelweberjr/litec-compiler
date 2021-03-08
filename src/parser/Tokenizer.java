package parser;

import common.defs.Symbol;
import common.defs.Tokens;
import common.defs.TokenType;

public class Tokenizer {
	public static void run(String text) throws Exception {
		for(int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			// skip whitespace
			if(ch == ' ' || ch == '\t' || ch =='\r') continue;
			if(ch == '\n') {
				Tokens.pushBack(new Tokens(TokenType.NL));
				continue;
			}
			
			// skip comments
			if(ch == '/') {
				if(text.charAt(i+1) == '/') {
					i += 2;
					while(true) { 
						if(text.charAt(i) == '\n') {
							Tokens.pushBack(new Tokens(TokenType.NL));
							break;
						}
						i++;
					}
					continue;
				}
				else if(text.charAt(i+1) == '*') {
					i += 2;
					while(true) {
						if(text.charAt(i) == '*' && text.charAt(i+1) == '/') {
							i++;
							break;
						}
						if(text.charAt(i) == '\n') Tokens.pushBack(new Tokens(TokenType.NL));
						i++;
					}
					continue;
				}
			}
			
			// do numbers first
			boolean neg = false;
			if(ch == '-' && Character.isDigit(text.charAt(i+1))) {
				neg = true;
				ch = text.charAt(++i);
			}
			
			if(Character.isDigit(ch))
			{
				int val = 0;
				while(Character.isDigit(text.charAt(i)))
				{
					val = val*10 + text.charAt(i) - '0';
					i++;
					if(i == text.length()) break;
				}
				
				if(neg) val = -val;
				
				i--;
				Tokens.pushBack(new Tokens(TokenType.NUM, val));
				continue;
			}
			
			// do symbols and keywords
			if(Character.isAlphabetic(ch))
			{
				String name = "";
				while(Character.isAlphabetic(ch) || Character.isDigit(ch))
				{
					name += ch;
					
					i++;
					if(i == text.length()) break;
					ch = text.charAt(i);
				}
				i--;
				
				switch(name)
				{
				case "if":
					Tokens.pushBack(new Tokens(TokenType.IF));
					break;
				case "else":
					Tokens.pushBack(new Tokens(TokenType.ELSE));
					break;
				case "for":
					Tokens.pushBack(new Tokens(TokenType.FOR));
					break;
				case "while":
					Tokens.pushBack(new Tokens(TokenType.WHILE));
					break;
				case "break":
					Tokens.pushBack(new Tokens(TokenType.BREAK));
					break;
				case "continue":
					Tokens.pushBack(new Tokens(TokenType.CONT));
					break;
				case "int":
					Tokens.pushBack(new Tokens(TokenType.INT));
					break;
				case "void":
					Tokens.pushBack(new Tokens(TokenType.VOID));
					break;
				case "return":
					Tokens.pushBack(new Tokens(TokenType.RET));
					break;
				default:
					Tokens.pushBack(new Tokens(TokenType.SYM, new Symbol(name)));
					break;
				}
				continue;
			}
			
			// do everything else last
			switch(ch) {
			case '(':
				Tokens.pushBack(new Tokens(TokenType.LPAR));
				break;
			case ')':
				Tokens.pushBack(new Tokens(TokenType.RPAR));
				break;
			case '{':
				Tokens.pushBack(new Tokens(TokenType.LBRACE));
				break;
			case '}':
				Tokens.pushBack(new Tokens(TokenType.RBRACE));
				break;
			case '=':
				if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.EQ));
				}
				else Tokens.pushBack(new Tokens(TokenType.ASGN));
				break;
			case '!':
				if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.NE));
				}
				else Tokens.pushBack(new Tokens(TokenType.NOT));
				break;
			case '<':
				if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.LE));
				}
				else if(text.charAt(i+1) == '<') {
					i++;
					if(text.charAt(i+1) == '=') {
						i++;
						Tokens.pushBack(new Tokens(TokenType.SHLEQ));
					}
					else Tokens.pushBack(new Tokens(TokenType.LSH));
				}
				else Tokens.pushBack(new Tokens(TokenType.LESS));
				break;
			case '>':
				if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.GE));
				}
				else if(text.charAt(i+1) == '>') {
					i++;
					if(text.charAt(i+1) == '=') {
						i++;
						Tokens.pushBack(new Tokens(TokenType.SHREQ));
					}
					else Tokens.pushBack(new Tokens(TokenType.RSH));
				}
				else Tokens.pushBack(new Tokens(TokenType.GRT));
				break;
			case '+':
				if(text.charAt(i+1) == '+') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.PP));
				}
				else if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.PLUSEQ));
				}
				else Tokens.pushBack(new Tokens(TokenType.PLUS));
				break;
			case '-':
				if(text.charAt(i+1) == '-') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.MM));
				}
				else if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.MINEQ));
				}
				else Tokens.pushBack(new Tokens(TokenType.MIN));
				break;
			case '*':
				if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.MULEQ));
				}
				else Tokens.pushBack(new Tokens(TokenType.MUL));
				break;
			case '/':
				if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.DIVEQ));
				}
				else Tokens.pushBack(new Tokens(TokenType.DIV));
				break;
			case '%':
				if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.MODEQ));
				}
				else Tokens.pushBack(new Tokens(TokenType.MOD));
				break;
			case ';':
				Tokens.pushBack(new Tokens(TokenType.SC));
				break;
			case '~':
				Tokens.pushBack(new Tokens(TokenType.BNOT));
				break;
			case '|':
				if(text.charAt(i+1) == '|') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.OR));
				}
				else if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.OREQ));
				}
				else Tokens.pushBack(new Tokens(TokenType.BOR));
				break;
			case '&':
				if(text.charAt(i+1) == '&') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.AND));
				}
				else if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.ANDEQ));
				}
				else Tokens.pushBack(new Tokens(TokenType.BAND));
				break;
			case '^':
				if(text.charAt(i+1) == '=') {
					i++;
					Tokens.pushBack(new Tokens(TokenType.XOREQ));
				}
				else Tokens.pushBack(new Tokens(TokenType.BXOR));
				break;
			case ',':
				Tokens.pushBack(new Tokens(TokenType.COMMA));
				break;
			default:
				throw new Exception("Unknown token: " + text.charAt(i));
			}
		}
	}	
	
}
