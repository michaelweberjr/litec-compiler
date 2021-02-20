package parser;

import java.util.ArrayList;

import parser.defs.FunctionTree;
import parser.defs.Token;
import parser.defs.TokenType;
import parser.util.Node;

public class Optimizer {
	public static void optimize(ArrayList<FunctionTree> program) {
		for(FunctionTree fn : program) 
			if(!fn.isBuiltIn) {
				solveLiterals(fn.statements.root);
			}
	}
	
	private static void solveLiterals(Node<Token> node) {
		for(int i = 0; i < node.children.size(); i++) {
			Node<Token> child = node.children.get(i);
			if(child != null) {
				solveLiterals(child);
				
				if(isMathToken(child.val.type)) {
					if(child.children.get(0).val.type == TokenType.NUM && child.children.get(1).val.type == TokenType.NUM) {
						switch(child.val.type) {
						case PLUS:
							node.children.set(i, new Node<Token>(new Token(TokenType.NUM, child.children.get(0).val.val + child.children.get(1).val.val)));
							break;
						case MIN:
							node.children.set(i, new Node<Token>(new Token(TokenType.NUM, child.children.get(0).val.val - child.children.get(1).val.val)));
							break;
						case MUL:
							node.children.set(i, new Node<Token>(new Token(TokenType.NUM, child.children.get(0).val.val * child.children.get(1).val.val)));
							break;
						case DIV:
							node.children.set(i, new Node<Token>(new Token(TokenType.NUM, child.children.get(0).val.val / child.children.get(1).val.val)));
							break;
						case MOD:
							node.children.set(i, new Node<Token>(new Token(TokenType.NUM, child.children.get(0).val.val % child.children.get(1).val.val)));
							break;
						default:
							break;
						}
					}
				}
			}
		}
	}

	private static boolean isMathToken(TokenType type) {
		return type == TokenType.PLUS || type == TokenType.MIN || type == TokenType.MUL || type == TokenType.DIV || type == TokenType.MOD;
	}
}
