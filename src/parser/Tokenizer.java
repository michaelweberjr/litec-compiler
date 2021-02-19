package parser;

import java.util.Deque;
import java.util.LinkedList;
import parser.defs.*;

public class Tokenizer {
	public Deque<Token> tokens;
	
	public Tokenizer(String text) throws Exception {
		tokens = new LinkedList<Token>();
		for(int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			// skip whitespace
			if(ch == ' ' || ch == '\t' || ch == '\n' || ch =='\r') continue;
			
			// do numbers first
			if(Character.isDigit(ch))
			{
				int val = 0;
				while(Character.isDigit(text.charAt(i)))
				{
					val = val*10 + text.charAt(i) - '0';
					i++;
					if(i == text.length()) break;
				}
				
				i--;
				tokens.add(new Token(TokenType.NUM, val));
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
				case "int":
					tokens.add(new Token(TokenType.INT));
					break;
				case "void":
					tokens.add(new Token(TokenType.VOID));
					break;
				case "return":
					tokens.add(new Token(TokenType.RET));
					break;
				default:
					tokens.add(new Token(TokenType.SYM, new Symbol(name)));
					break;
				}
				continue;
			}
			
			// do everything else last
			switch(ch) {
			case '(':
				tokens.add(new Token(TokenType.LPAR));
				break;
			case ')':
				tokens.add(new Token(TokenType.RPAR));
				break;
			case '{':
				tokens.add(new Token(TokenType.LBRA));
				break;
			case '}':
				tokens.add(new Token(TokenType.LBRA));
				break;
			case '=':
				tokens.add(new Token(TokenType.EQ));
				break;
			case '+':
				tokens.add(new Token(TokenType.PLUS));
				break;
			case '-':
				tokens.add(new Token(TokenType.MIN));
				break;
			case '*':
				tokens.add(new Token(TokenType.MUL));
				break;
			case '/':
				tokens.add(new Token(TokenType.DIV));
				break;
			case '%':
				tokens.add(new Token(TokenType.MOD));
				break;
			case ';':
				tokens.add(new Token(TokenType.SC));
				break;
			case ',':
				tokens.add(new Token(TokenType.COMMA));
				break;
			default:
				throw new Exception("Unknown token: " + text.charAt(i));
			}
		}
	}
	
	public void printTokens() {
		for(Token token : tokens)
		{
			switch(token.type)
			{
			case LPAR:
				System.out.print("TOKEN_LPAR\n");
				break;
			case RPAR:
				System.out.print("TOKEN_RPAR\n");
				break;
			case LBRA:
				System.out.print("TOKEN_LBRA\n");
				break;
			case RBRA:
				System.out.print("TOKEN_RBRA\n");
				break;
			case EQ:
				System.out.print("TOKEN_EQ\n");
				break;
			case PLUS:
				System.out.print("TOKEN_PLUS\n");
				break;
			case MIN:
				System.out.print("TOKEN_MIN\n");
				break;
			case MUL:
				System.out.print("TOKEN_MUL\n");
				break;
			case DIV:
				System.out.print("TOKEN_DIV\n");
				break;
			case MOD:
				System.out.print("TOKEN_MOD\n");
				break;
			case SC:
				System.out.print("TOKEN_SC\n");
				break;
			case INT:
				System.out.print("TOKEN_INT\n");
				break;
			case NUM:
				System.out.print("TOKEN_NUM: ");
				System.out.println(token.val);
				break;
			case RET:
				System.out.print("TOKEN_RET\n");
				break;
			case SYM:
				System.out.print("TOKEN_SYM: ");
				System.out.println(token.sym.name);
				break;
			case VOID:
				System.out.print("TOKEN_VOID\n");
				break;
			case COMMA:
				System.out.print("TOKEN_COMMA");
				break;
			default:
				break;
			}
		}
	}
}