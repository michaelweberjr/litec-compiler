package parser;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;

import common.defs.Registers;
import common.defs.FunctionTree;
import common.defs.Symbol;
import common.defs.Token;
import common.defs.TokenType;
import common.util.Node;

public class Parser {
	public ArrayList<FunctionTree> program;
	
	public Parser(Deque<Token> tokens) throws Exception {
		program = new ArrayList<FunctionTree>();
		addBuiltInFunctions();
		
		while(!tokens.isEmpty()) {
			parseFunction(tokens);
		}
	}
	
	private void parseFunction(Deque<Token> tokens) throws Exception {
		popTokenNL(tokens);
		Token t = tokens.peek();
		
		if(t.type == TokenType.INT || t.type == TokenType.VOID) {
			FunctionTree fn = new FunctionTree();
			fn.returnType = tokens.remove();
			fn.name = popToken(tokens, TokenType.SYM, "Expected function name").sym;
			fn.name.setFn();
			
			for(FunctionTree fns : program)
				if(fns.name.name.equals(fn.name.name))
					throw new Exception("Function " + fn.name.name + " has already been defined");
			
			popToken(tokens, TokenType.LPAR, "Missing \'(\' in function defintion");
			
			if(tokens.peek().type != TokenType.RPAR) {
				while(true) {
					popToken(tokens, TokenType.INT, "Expected variable type in function definition");
					fn.argsList.add(popToken(tokens, TokenType.SYM, "Expected variable name in function difinition").sym);
					popTokenNL(tokens);
					if(tokens.peek().type == TokenType.COMMA) {
						tokens.poll();
					}
					else {
						break;
					}
				}
			}
			
			popToken(tokens, TokenType.RPAR, "Missing \')\' in function defintion");
			popToken(tokens, TokenType.LBRACE, "Missing \'{\' in function defintion");
			
			popTokenNL(tokens);
			while(tokens.peek().type != TokenType.RBRACE) {
				parseStatement(tokens, fn, fn.statements.root);
			}
			
			popToken(tokens, TokenType.RBRACE, "Missing \'}\' in function defintion");
			
			if(fn.returnType.type != TokenType.VOID && !fn.hasReturn) 
				throw new ParserError("Missing return statement in function: " + fn.name.name);
			
			program.add(fn);
		}
		else throw new ParserError("Missing type for function definition");
	}
	
	private void parseStatement(Deque<Token> tokens, FunctionTree fn, Node<Token> node) throws Exception {
		popTokenNL(tokens);
		Token t = tokens.peek();	
		if(t.type == TokenType.RBRACE || t.type == TokenType.SC) return;
		
		if(t.type == TokenType.INT) parseVariableDec(tokens, fn, node);
		else if(t.type == TokenType.SYM) {
			boolean isFn = false;
			FunctionTree callee = null;
			for(FunctionTree fns : program) {
				if(fns.name.name.equals(t.sym.name)) {
					isFn = true;
					callee = fns;
					break;
				}
			}
			
			if(isFn) parseFunctionCall(tokens, fn, callee, node);
			else parseAssignment(tokens, fn, node);
		}
		else if(t.type == TokenType.RET) {
			parseReturn(tokens, fn, node);
		}
		
		popToken(tokens, TokenType.SC, "Expected \';\' at end of statement");
	}
	
	private void parseAssignment(Deque<Token> tokens, FunctionTree fn, Node<Token> node) throws Exception {
		int varOffset = -1;
		for(int i = 0; i < fn.frameVar.size(); i++) {
			Symbol var = fn.frameVar.get(i);
			if(var.name.equals(tokens.peek().sym.name)) {
				varOffset = i;
				break;
			}
		}
		
		if(varOffset < 0)
			for(int i = 0; i < fn.argsList.size(); i++) {
				Symbol var = fn.argsList.get(i);
				if(var.name.equals(tokens.peek().sym.name)) {
					varOffset = Registers.preserveRegisters.length + fn.frameVar.size() + 1 + i;
					break;
				}
			}
		
		if(varOffset < 0) throw new ParserError("Undefined identifier: " + tokens.peek().sym.name);
		
		
		Token var = tokens.poll();
		Node<Token> n = new Node<Token>(popToken(tokens, TokenType.EQ, "Expected \'=\' after variable name"));
		node.addChild(n);
		var.val = varOffset;
		n.addChild(var);
		parseMathExp(tokens, fn, n);
	}
	
	private void parseVariableDec(Deque<Token> tokens, FunctionTree fn, Node<Token> node) throws Exception {
		tokens.poll();
		popTokenNL(tokens);
		if(tokens.peek().type != TokenType.SYM) throw new ParserError("Expceted variable name");
		
		boolean foundVar = false;
		for(Symbol var : fn.frameVar) 
			if(var.name.equals(tokens.peek().sym.name)) {
				foundVar = true;
				break;
			}
		
		if(!foundVar)
			for(Symbol var : fn.argsList) 
				if(var.name.equals(tokens.peek().sym.name)) {
					foundVar = true;
					break;
				}
		
		if(foundVar) throw new ParserError("Redifinition of identifier: " + tokens.peek().sym.name);
		
		fn.frameVar.add(tokens.peek().sym);
		tokens.peek().sym.setVar();
		
		Token t = tokens.poll();
		popTokenNL(tokens);
		if(tokens.peek().type == TokenType.EQ) {
			tokens.addFirst(t);
			parseAssignment(tokens, fn, node);
		}
	}
	
	private void parseFunctionCall(Deque<Token> tokens, FunctionTree fn, FunctionTree callee, Node<Token> node) throws Exception {
		Node<Token> n = new Node<Token>(new Token(TokenType.CALL));
		node.addChild(n);
		n.addChild(tokens.poll());
		popToken(tokens, TokenType.LPAR, "Expceted \'(\' in function call");
		while(tokens.peek().type != TokenType.RPAR) {
			popTokenNL(tokens);
			parseMathExp(tokens, fn, n);
			popTokenNL(tokens);
			if(tokens.peek().type == TokenType.COMMA) tokens.poll();
		}
		if(n.children.size() != callee.argsList.size()+1)
			throw new ParserError("Function call to " + callee.name.name + " with " + n.children.size() + " arguments, function call requires " + callee.argsList.size());
		popToken(tokens, TokenType.RPAR, "Expceted \')\' in function call");
	}
	
	private void parseReturn(Deque<Token> tokens, FunctionTree fn, Node<Token> node) throws Exception {
		tokens.poll();
		Node<Token> n = new Node<Token>(new Token(TokenType.RET));
		node.addChild(n);
		popTokenNL(tokens);
		if(tokens.peek().type == TokenType.SC) {
			if(fn.returnType.type == TokenType.VOID) {
				fn.hasReturn = true;
				return;
			}
			else throw new ParserError("Function " + fn.name.name + "requires a math expresion when returning"); 
		}
		
		parseMathExp(tokens, fn, n);
		fn.hasReturn = true;
	}
	
	private void parseMathExp(Deque<Token> tokens, FunctionTree fn, Node<Token> node) throws Exception {
		Deque<Node<Token>> stack = new LinkedList<Node<Token>>();
		Deque<Node<Token>> output = new LinkedList<Node<Token>>();
		Node<Token> nodeTest = new Node<Token>(new Token(TokenType.LPAR));
		TokenType last = TokenType.ROOT;
		
		while(true) {
			popTokenNL(tokens);
			Token t = tokens.poll();
			if(t.type == TokenType.SC || t.type == TokenType.COMMA || (t.type == TokenType.RPAR && !stack.contains(nodeTest))) {
				tokens.addFirst(t);
				break;
			}
			
			if((t.type == TokenType.NUM || t.type == TokenType.SYM || t.type == TokenType.CALL) && (last == TokenType.NUM || last == TokenType.SYM || last == TokenType.CALL || last == TokenType.RPAR))
				throw new ParserError("expected math operation after literal type");
			if(Token.isMathToken(t.type) && Token.isMathToken(last))
				throw new ParserError("expected literal after math operation");
			last = t.type;
			
			if(t.type == TokenType.NUM) {
				output.add(new Node<Token>(t));
			}
			else if(t.type == TokenType.SYM) {
				int varOffset = -1;
				for(int i = 0; i < fn.argsList.size(); i++) {
					Symbol s = fn.argsList.get(i);
					if(s.name.equals(t.sym.name)) {
						varOffset = Registers.preserveRegisters.length + fn.frameVar.size() + 1 + i;
						break;
					}
				}
				
				if(varOffset < 0)
					for(int i = 0; i < fn.frameVar.size(); i++) {
						Symbol s = fn.frameVar.get(i);
						if(s.name.equals(t.sym.name)) {
							varOffset = i;
							break;
						}
					}
				
				if(varOffset >= 0) {
					t.val = varOffset;
					output.add(new Node<Token>(t));
					continue;
				}
				
				FunctionTree callee = null;
				for(FunctionTree fns : program) {
					if(fns.name.name.equals(t.sym.name)) {
						callee = fns;
						break;
					}
				}
				
				if(callee == null) throw new Exception("Undefined identifer: " + t.sym.name);
				
				Node<Token> n = new Node<Token>(new Token(TokenType.ROOT));
				tokens.addFirst(t);
				parseFunctionCall(tokens, fn, callee, n);
				output.add(n.children.get(0));
			}
			else if(t.type == TokenType.LPAR) {
				stack.add(new Node<Token>(t));
			}
			else if(t.type == TokenType.RPAR) {
				while(true) {
					if(stack.isEmpty()) throw new Exception("Found unbalaced \')\'");
					output.add(stack.pollLast());
					if(stack.peekLast().val.type == TokenType.LPAR) {
						stack.pollLast();
						break;
					}
				}
			}
			else {
				if(stack.isEmpty() || evaluationLevel(t.type) > evaluationLevel(stack.peekLast().val.type)) {
					stack.add(new Node<Token>(t));
				}
				else {
					while(!stack.isEmpty() && evaluationLevel(t.type) <= evaluationLevel(stack.peekLast().val.type)) {
						output.add(stack.pollLast());
					}
					stack.add(new Node<Token>(t));
				}
			}
		}
		
		while(!stack.isEmpty()) output.add(stack.pollLast());
		buildMathTree(output, node);
	}
	
	private Token popToken(Deque<Token> tokens, TokenType type, String errorMsg) throws Exception {
		popTokenNL(tokens);
		Token t = tokens.poll();
		if(t == null) throw new ParserError(errorMsg);
		if(t.type != type) throw new ParserError(errorMsg);
		return t;
	}
	
	private void popTokenNL(Deque<Token> tokens) {
		while(tokens.peek().type == TokenType.NL) {
			ParserError.lineNum++;
			tokens.poll();
		}
	}
	
	private int evaluationLevel(TokenType type) throws Exception {
		switch(type) {
		case LPAR:
			return 0;
		case PLUS:
		case MIN:
			return 1;
		case MUL:
		case DIV:
		case MOD:
			return 2;
		case CALL:
			return 3;
		default:
			throw new ParserError("Bad token");
		}
	}
	
	private void buildMathTree(Deque<Node<Token>> stack, Node<Token> node) throws Exception {
		if(stack.isEmpty()) return;
		
		node.addChild(stack.pollLast());
		node = node.children.get(node.children.size()-1);
		if(Token.isMathToken(node.val.type)) {
			buildMathTree(stack, node);
			buildMathTree(stack, node);
		}
	}
	
	private void addBuiltInFunctions() {
		program.add(BuiltInFunction.printFnTree());
	}
	
	public void printParserTree() {
		for(FunctionTree fn : program) {
			System.out.println("============================================================");
			System.out.println(fn.name.name + ": " + (fn.returnType.type == TokenType.VOID ? "void" : "int"));
			System.out.println("Args count: " + fn.argsList.size());
			System.out.println("Local var count: " + fn.frameVar.size());
			System.out.println("Statement count: " + fn.statements.root.children.size());
			System.out.println("============================================================\n");
		}
	}
}
