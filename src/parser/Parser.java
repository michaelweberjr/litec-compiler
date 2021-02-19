package parser;

import java.util.ArrayList;
import java.util.Deque;

import parser.defs.*;
import parser.util.*;

public class Parser {
	public ArrayList<FunctionTree> program;
	
	public Parser(Deque<Token> tokens) throws Exception {
		while(!tokens.isEmpty()) {
			parseFunction(tokens);
		}
	}
	
	private void parseFunction(Deque<Token> tokens) throws Exception {
		Token t = tokens.peek();
		if(t.type == TokenType.INT || t.type == TokenType.VOID) {
			FunctionTree fn = new FunctionTree();
			fn.returnType = tokens.remove();
			fn.name = popToken(tokens, TokenType.SYM, "Expected function name").sym;
			fn.name.setFn();
			
			popToken(tokens, TokenType.LPAR, "Missing \'(\' in function defintion");
			
			if(tokens.peek().type != TokenType.RPAR) {
				while(true) {
					popToken(tokens, TokenType.INT, "Expected variable type in function definition");
					fn.argsList.add(popToken(tokens, TokenType.SYM, "Expected variable name in function difinition").sym);
					if(tokens.peek().type == TokenType.COMMA) {
						tokens.poll();
					}
					else {
						break;
					}
				}
			}
			
			popToken(tokens, TokenType.RPAR, "Missing \')\' in function defintion");
			popToken(tokens, TokenType.LBRA, "Missing \'{\' in function defintion");
			
			
			while(tokens.peek().type != TokenType.RBRA) {
				parseStatement(tokens, fn, fn.statements.root);
			}
			
			popToken(tokens, TokenType.RBRA, "Missing \'}\' in function defintion");
			
			if(fn.returnType.type != TokenType.VOID && !fn.hasReturn) 
				throw new Exception("Missing return statement in function: " + fn.name.name);
			
			program.add(fn);
		}
		else throw new Exception("Missing type for function definition");
	}
	
	private void parseStatement(Deque<Token> tokens, FunctionTree fn, Node<Token> root) throws Exception {
		Token t = tokens.peek();
		if(t.type == TokenType.RBRA || t.type == TokenType.SC) return;
		
		if(t.type ==TokenType.INT) parseVariableDec(tokens, fn, root);
		else if(t.type == TokenType.SYM) {
			boolean isFn = false;
			FunctionTree callee = null;
			for(FunctionTree fns : program) {
				if(fns.name.name == t.sym.name) {
					isFn = true;
					callee = fns;
					break;
				}
			}
			
			if(isFn) parseFunctionCall(tokens, callee, root);
			else parseAssignment(tokens, fn, root);
		}
		else if(t.type == TokenType.RET) {
			parseReturn(tokens, fn, root);
		}
		
		popToken(tokens, TokenType.SC, "Expected \';\' at end of statement");
	}
	
	private void parseAssignment(Deque<Token> tokens, FunctionTree fn, Node<Token> root) throws Exception {
		boolean notFoundVar = true;
		for(Symbol var : fn.frameVar) 
			if(var.name == tokens.peek().sym.name) {
				notFoundVar = false;
				break;
			}
		
		if(notFoundVar)
			for(Symbol var : fn.argsList) 
				if(var.name == tokens.peek().sym.name) {
					notFoundVar = false;
					break;
				}
		
		if(notFoundVar) throw new Exception("Undefined identifier: " + tokens.peek().sym.name);
		
		Node<Token> n = new Node<Token>(new Token(TokenType.EQ));
		root.addChild(n);
		n.addChild(tokens.poll());
		popToken(tokens, TokenType.EQ, "Expected \'=\' after variable name");
		parseMathExp(tokens, n);
	}
	
	private void parseVariableDec(Deque<Token> tokens, FunctionTree fn, Node<Token> root) throws Exception {
		tokens.poll();
		if(tokens.peek().type != TokenType.SYM) throw new Exception("Expceted variable name");
		
		boolean notFoundVar = true;
		for(Symbol var : fn.frameVar) 
			if(var.name == tokens.peek().sym.name) {
				notFoundVar = false;
				break;
			}
		
		if(notFoundVar)
			for(Symbol var : fn.argsList) 
				if(var.name == tokens.peek().sym.name) {
					notFoundVar = false;
					break;
				}
		
		if(!notFoundVar) throw new Exception("Redifinition of identifier: " + tokens.peek().sym.name);
		
		fn.frameVar.add(tokens.peek().sym);
		tokens.peek().sym.setVar();
		
		Token t = tokens.poll();
		if(tokens.peek().type == TokenType.EQ) {
			tokens.addFirst(t);
			parseAssignment(tokens, fn, root);
		}
	}
	
	private void parseFunctionCall(Deque<Token> tokens, FunctionTree callee, Node<Token> root) throws Exception {
		Node<Token> n = new Node<Token>(new Token(TokenType.CALL));
		root.addChild(n);
		n.addChild(tokens.poll());
		popToken(tokens, TokenType.LPAR, "Expceted \'(\' in function call");
		while(tokens.peek().type != TokenType.RPAR) {
			parseMathExp(tokens, n);
			if(tokens.peek().type == TokenType.COMMA) tokens.poll();
		}
		if(n.children.size() != callee.argsList.size())
			throw new Exception("Function call to " + callee.name.name + " with " + n.children.size() + " arguments, function call requires " + callee.argsList.size());
		popToken(tokens, TokenType.LPAR, "Expceted \')\' in function call");
	}
	
	private void parseReturn(Deque<Token> tokens, FunctionTree fn, Node<Token> root) throws Exception {
		tokens.poll();
		Node<Token> n = new Node<Token>(new Token(TokenType.RET));
		root.addChild(n);
		if(tokens.peek().type == TokenType.SC) {
			if(fn.returnType.type == TokenType.VOID) {
				fn.hasReturn = true;
				return;
			}
			else throw new Exception("Function " + fn.name.name + "requires a math expresion when returning"); 
		}
		
		parseMathExp(tokens, n);
		fn.hasReturn = true;
	}
	
	private void parseMathExp(Deque<Token> tokens, Node<Token> root) throws Exception {
		
	}
	
	private Token popToken(Deque<Token> tokens, TokenType type, String errorMsg) throws Exception {
		Token t = tokens.poll();
		if(t == null) throw new Exception(errorMsg);
		if(t.type != type) throw new Exception(errorMsg);
		return t;
	}
	
	public void printParserTree() {
		
	}
}
