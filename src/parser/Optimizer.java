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
				fasterInstructions(fn.statements.root);
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
						case NOT:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val == 0 ? 1 : 0)));
							break;
						case NEG:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, -child.children.get(0).val.val)));
							break;
						case BNOT:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, ~child.children.get(0).val.val)));
							break;
						case PLUS:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val + child.children.get(1).val.val)));
							break;
						case MIN:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(1).val.val - child.children.get(0).val.val)));
							break;
						case MUL:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val * child.children.get(1).val.val)));
							break;
						case DIV:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(1).val.val / child.children.get(0).val.val)));
							break;
						case MOD:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(1).val.val % child.children.get(0).val.val)));
							break;
						case BOR:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val | child.children.get(1).val.val)));
							break;
						case BAND:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val & child.children.get(1).val.val)));
							break;
						case BXOR:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(0).val.val ^ child.children.get(1).val.val)));
							break;
						case LSH:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(1).val.val << child.children.get(0).val.val)));
							break;
						case RSH:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, child.children.get(1).val.val >> child.children.get(0).val.val)));
							break;
						case EQ:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, (child.children.get(0).val.val == child.children.get(1).val.val) ? 1 : 0)));
							break;
						case NE:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, (child.children.get(0).val.val != child.children.get(1).val.val) ? 1 : 0)));
							break;
						case LESS:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, (child.children.get(1).val.val < child.children.get(0).val.val) ? 1 : 0)));
							break;
						case GRT:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, (child.children.get(1).val.val > child.children.get(0).val.val) ? 1 : 0)));
							break;
						case LE:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, (child.children.get(1).val.val <= child.children.get(0).val.val) ? 1 : 0)));
							break;
						case GE:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, (child.children.get(1).val.val >= child.children.get(0).val.val) ? 1 : 0)));
							break;
						case OR:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, (child.children.get(0).val.val != 0) || (child.children.get(1).val.val != 0) ? 1 : 0)));
							break;
						case AND:
							node.children.set(i, new Node<Tokens>(new Tokens(TokenType.NUM, (child.children.get(0).val.val != 0) && (child.children.get(1).val.val != 0) ? 1 : 0)));
							break;
						default:
							break;
						}
					}
				}
			}
		}
	}

	private static void fasterInstructions(Node<Tokens> node) {
		for(int i = 0; i < node.children.size(); i++) {
			Node<Tokens> child = node.children.get(i);
			if(child != null) {
				fasterInstructions(child);
				
				if(child.val.type == TokenType.ASGN) {
					if(child.children.get(1).val.type == TokenType.NUM && child.children.get(1).val.val == 0) {
						child.children.set(1,  child.children.get(0));
						child.val.type = TokenType.XOREQ;
					}
				}
				else if(child.val.type == TokenType.MUL) {
					if(child.children.get(0).val.type == TokenType.NUM) {
						if(isPowerOf2(child.children.get(0).val.val)) {
							child.val.type = TokenType.LSH;
							child.children.get(0).val.val = getPowerOf2(child.children.get(0).val.val);
						}
					}
					else if(child.children.get(1).val.type == TokenType.NUM) {
						if(isPowerOf2(child.children.get(1).val.val)) {
							child.val.type = TokenType.LSH;
							int shift = getPowerOf2(child.children.get(1).val.val);
							child.children.set(1, child.children.get(0));
							child.children.set(0, new Node<Tokens>(new Tokens(TokenType.NUM, shift)));
						}
					}
				}
				else if(child.val.type == TokenType.DIV) {
					if(child.children.get(0).val.type == TokenType.NUM) {
						if(isPowerOf2(child.children.get(0).val.val)) {
							child.val.type = TokenType.RSH;
							child.children.get(0).val.val = getPowerOf2(child.children.get(0).val.val);
						}
					}
				}
				else if(child.val.type == TokenType.PLUSEQ && child.children.get(1).val.type == TokenType.NUM && child.children.get(1).val.val == 1) {
					child.val.type = TokenType.PP;
					child.children.remove(1);
				}
				else if(child.val.type == TokenType.MINEQ && child.children.get(1).val.type == TokenType.NUM && child.children.get(1).val.val == 1) {
					child.val.type = TokenType.MM;
					child.children.remove(1);
				}
			}
		}
	}
	
	private static boolean isPowerOf2(long num) {
		if(num < 2 && num % 2 == 1) return false;
		while(num > 2) {
			if(num % 2 == 1) return false;
			num /= 2;
		}
		return true;
	}
	
	private static int getPowerOf2(long num) {
		int count = 1;
		while(num > 2) {
			count++;
			num /= 2;
		}
		return count;
	}
}
