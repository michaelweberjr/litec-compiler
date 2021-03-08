package parser;

import java.util.ArrayList;

import common.defs.FunctionTree;
import common.defs.Tokens;
import common.defs.TokenType;
import common.util.Node;

public class Optimizer {
	public static void optimize(ArrayList<FunctionTree> program) {
		for(FunctionTree fn : program) 
			if(!fn.isBuiltIn) {
				solveLiterals(fn.statements.root);
			}
	}
	
	private static void solveLiterals(Node<Tokens> node) {
		for(int i = 0; i < node.children.size(); i++) {
			Node<Tokens> child = node.children.get(i);
			if(child != null) {
				solveLiterals(child);
				
				if(Tokens.isMathToken(child.val.type) && Tokens.mathArgCount(child.val.type) > 1) {
					if(child.children.get(0).val.type == TokenType.NUM && child.children.get(1).val.type == TokenType.NUM) {
						switch(child.val.type) {
						case PLUS:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val + child.children.get(1).val.val)));
							break;
						case MIN:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val - child.children.get(1).val.val)));
							break;
						case MUL:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val * child.children.get(1).val.val)));
							break;
						case DIV:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val / child.children.get(1).val.val)));
							break;
						case MOD:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val % child.children.get(1).val.val)));
							break;
						default:
							break;
						}
					}
				}
			}
		}
	}
}
