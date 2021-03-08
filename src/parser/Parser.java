package parser;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Stack;

import common.defs.Registers;
import common.defs.FunctionTree;
import common.defs.Symbol;
import common.defs.Tokens;
import common.defs.TokenType;
import common.util.Node;

public class Parser {
	public static ArrayList<FunctionTree> program;
	private static Stack<Node<Tokens>> loopStack = new Stack<>();
	
	public static void run() throws Exception {
		program = new ArrayList<FunctionTree>();
		addBuiltInFunctions();
		
		while(!Tokens.isEmpty()) {
			parseFunction();
		}
	}
	
	private static void parseFunction() throws Exception {
		Tokens t = Tokens.peekFront();
		
		if(t.type == TokenType.INT || t.type == TokenType.VOID) {
			FunctionTree fn = new FunctionTree();
			fn.returnType = Tokens.popFront();
			fn.name = popToken(TokenType.SYM, "Expected function name").sym;
			fn.name.setFn();
			
			for(FunctionTree fns : program)
				if(fns.name.name.equals(fn.name.name))
					throw new Exception("Function " + fn.name.name + " has already been defined");
			program.add(fn);
			
			popToken(TokenType.LPAR, "Missing \'(\' in function defintion");
			
			if(Tokens.peekFront().type != TokenType.RPAR) {
				while(true) {
					popToken(TokenType.INT, "Expected variable type in function definition");
					fn.argsList.add(popToken(TokenType.SYM, "Expected variable name in function difinition").sym);
					if(Tokens.peekFront().type == TokenType.COMMA) {
						Tokens.popFront();
					}
					else {
						break;
					}
				}
			}
			
			popToken(TokenType.RPAR, "Missing \')\' in function defintion");
			popToken(TokenType.LBRACE, "Missing \'{\' in function defintion");
			
			while(Tokens.peekFront().type != TokenType.RBRACE) {
				parseStatement(fn, fn.statements.root);
			}
			
			popToken(TokenType.RBRACE, "Missing \'}\' in function defintion");
			
			if(fn.returnType.type != TokenType.VOID && !fn.hasReturn) 
				throw new ParserError("Missing return statement in function: " + fn.name.name);
		}
		else throw new ParserError("Missing type for function definition");
	}
	
	private static void parseStatement(FunctionTree fn, Node<Tokens> node) throws Exception {
		Tokens t = Tokens.peekFront();	
		if(t.type == TokenType.RBRACE || t.type == TokenType.SC) return;
		
		boolean found = false;
		if(t.type == TokenType.INT) {
			parseVariableDec(fn, node);
			popToken(TokenType.SC, "Expected \';\' at end of statement");
			found = true;
		}
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
			
			if(isFn) parseFunctionCall(fn, callee, node);
			else {
				Tokens save = Tokens.popFront();
				if(Tokens.peekFront().type == TokenType.PP || Tokens.peekFront().type == TokenType.MM) {
					Tokens.pushFront(save);
					parseIncDec(fn, node);
				}
				else {
					Tokens.pushFront(save);
					parseAssignment(fn, node);
				}
			}
			popToken(TokenType.SC, "Expected \';\' at end of statement");
			found = true;
		}
		else if(t.type == TokenType.PP || t.type == TokenType.MM) {
			parseIncDec(fn, node);
			popToken(TokenType.SC, "Expected \';\' at end of statement");
			found = true;
		}
		else if(t.type == TokenType.RET) {
			parseReturn(fn, node);
			popToken(TokenType.SC, "Expected \';\' at end of statement");
			found = true;
		}
		else if(t.type == TokenType.BREAK) {
			Node<Tokens> n = new Node<>(Tokens.popFront());
			if(loopStack.isEmpty()) throw new Exception("No loop to break from");
			n.children.add(loopStack.peek());
			node.addChild(n);
			popToken(TokenType.SC, "Expected \';\' at end of statement");
			found = true;
		}
		else if(t.type == TokenType.CONT) {
			Node<Tokens> n = new Node<>(Tokens.popFront());
			if(loopStack.isEmpty()) throw new Exception("Not loop to continue from");
			n.children.add(loopStack.peek());
			node.addChild(n);
			popToken(TokenType.SC, "Expected \';\' at end of statement");
			found = true;
		}
		
		if(t.type == TokenType.IF) {
			parseIf(fn, node);
			found = true;
		}
		else if(t.type == TokenType.FOR) {
			parseFor(fn, node);
			found = true;
		}
		else if(t.type == TokenType.WHILE) {
			parseWhile(fn, node);
			found = true;
		}
		
		if(!found) throw new ParserError("Unexcpted token");
	}

	private static void parseAssignment(FunctionTree fn, Node<Tokens> node) throws Exception {
		int varOffset = -1;
		for(int i = 0; i < fn.frameVar.size(); i++) {
			Symbol var = fn.frameVar.get(i);
			if(var.name.equals(Tokens.peekFront().sym.name)) {
				varOffset = i;
				break;
			}
		}
		
		if(varOffset < 0)
			for(int i = 0; i < fn.argsList.size(); i++) {
				Symbol var = fn.argsList.get(i);
				if(var.name.equals(Tokens.peekFront().sym.name)) {
					varOffset = Registers.preserveRegisters.length + fn.frameVar.size() + 1 + i;
					break;
				}
			}
		
		if(varOffset < 0) throw new ParserError("Undefined identifier: " + Tokens.peekFront().sym.name);
		
		
		Tokens var = Tokens.popFront();
		if(!Tokens.isOpEq(Tokens.peekFront().type)) throw new ParserError("Expected \'=\' after variable name");
		Node<Tokens> n = new Node<Tokens>(Tokens.popFront());
		node.addChild(n);
		var.val = varOffset;
		n.addChild(var);
		parseMathExp(fn, n);
	}
	
	private static void parseVariableDec(FunctionTree fn, Node<Tokens> node) throws Exception {
		Tokens.popFront();
		if(Tokens.peekFront().type != TokenType.SYM) throw new ParserError("Expceted variable name");
		
		boolean foundVar = false;
		for(Symbol var : fn.frameVar) 
			if(var.name.equals(Tokens.peekFront().sym.name)) {
				foundVar = true;
				break;
			}
		
		if(!foundVar)
			for(Symbol var : fn.argsList) 
				if(var.name.equals(Tokens.peekFront().sym.name)) {
					foundVar = true;
					break;
				}
		
		if(foundVar) throw new ParserError("Redifinition of identifier: " + Tokens.peekFront().sym.name);
		
		fn.frameVar.add(Tokens.peekFront().sym);
		Tokens.peekFront().sym.setVar();
		
		Tokens t = Tokens.popFront();
		if(Tokens.isOpEq(Tokens.peekFront().type)) {
			Tokens.pushFront(t);
			parseAssignment(fn, node);
		}
	}
	
	private static void parseIf(FunctionTree fn, Node<Tokens> node) throws Exception {
		Node<Tokens> n = new Node<Tokens>(popToken(TokenType.IF, "Missing if decleration"));
		popToken(TokenType.LPAR, "Missing \'(\' in if statement");
		parseMathExp(fn, n);
		popToken(TokenType.RPAR, "Missing \')\' in if statement");
		parseCodeBlock(fn, n);
		node.addChild(n);
		
		while(Tokens.peekFront().type == TokenType.ELSE) {
			n = new Node<>(Tokens.popFront());
			if(Tokens.peekFront().type == TokenType.IF) {
				n.val.type = TokenType.ELIF;
				Tokens.popFront();
				popToken(TokenType.LPAR, "Missing \'(\' in if statement");
				parseMathExp(fn, n);
				popToken(TokenType.RPAR, "Missing \')\' in if statement");
			}
			
			parseCodeBlock(fn, n);
			node.addChild(n);
			if(n.val.type == TokenType.ELSE) break;
		}
	}
	
	private static void parseFor(FunctionTree fn, Node<Tokens> node) throws Exception {
		Node<Tokens> n = new Node<>(popToken(TokenType.FOR, "Missing for decleration"));
		loopStack.add(n);
		popToken(TokenType.LPAR, "Missing \'(\' in if statement");
		if(Tokens.peekFront().type == TokenType.SC) {
			Tokens.popFront();
			n.addChild(new Node<>(new Tokens(TokenType.ROOT)));
		}
		else parseStatement(fn, n);
		
		if(Tokens.peekFront().type == TokenType.SC) {
			Tokens.popFront();
			n.addChild(new Node<>(new Tokens(TokenType.ROOT)));
		}
		else parseStatement(fn, n);
		
		parseMathExp(fn, n);
		popToken(TokenType.RPAR, "Missing \')\' in if statement");
		
		parseCodeBlock(fn, n);
		node.addChild(n);
		loopStack.pop();
	}
	
	private static void parseWhile(FunctionTree fn, Node<Tokens> node) throws Exception {
		Node<Tokens> n = new Node<>(popToken(TokenType.WHILE, "Missing while decleration"));
		n.val.type = TokenType.FOR;
		loopStack.add(n);
		popToken(TokenType.LPAR, "Missing \'(\' in if statement");
		n.addChild(new Node<>(new Tokens(TokenType.ROOT)));
		parseMathExp(fn, n);
		n.addChild(new Node<>(new Tokens(TokenType.ROOT)));
		popToken(TokenType.RPAR, "Missing \')\' in if statement");
		
		parseCodeBlock(fn, n);
		node.addChild(n);
		loopStack.pop();
	}
	
	private static void parseCodeBlock(FunctionTree fn, Node<Tokens> node) throws Exception {
		if(Tokens.peekFront().type != TokenType.LBRACE) parseStatement(fn, node);
		else {
			popToken(TokenType.LBRACE, "Missing \'{\' in statement");
			while(Tokens.peekFront().type != TokenType.RBRACE) {
				parseStatement(fn, node);
			}
			Tokens.popFront();
		}
	}
	
	private static void parseFunctionCall(FunctionTree fn, FunctionTree callee, Node<Tokens> node) throws Exception {
		Node<Tokens> n = new Node<Tokens>(new Tokens(TokenType.CALL));
		node.addChild(n);
		n.addChild(Tokens.popFront());
		popToken(TokenType.LPAR, "Expceted \'(\' in function call");
		while(Tokens.peekFront().type != TokenType.RPAR) {
			parseMathExp(fn, n);
			if(Tokens.peekFront().type == TokenType.COMMA) Tokens.popFront();
		}
		if(n.children.size() != callee.argsList.size()+1)
			throw new ParserError("Function call to " + callee.name.name + " with " + n.children.size() + " arguments, function call requires " + callee.argsList.size());
		popToken(TokenType.RPAR, "Expceted \')\' in function call");
	}
	
	private static void parseReturn(FunctionTree fn, Node<Tokens> node) throws Exception {
		Tokens.popFront();
		Node<Tokens> n = new Node<Tokens>(new Tokens(TokenType.RET));
		node.addChild(n);
		if(Tokens.peekFront().type == TokenType.SC) {
			if(fn.returnType.type == TokenType.VOID) {
				fn.hasReturn = true;
				return;
			}
			else throw new ParserError("Function " + fn.name.name + "requires a math expresion when returning"); 
		}
		
		parseMathExp(fn, n);
		fn.hasReturn = true;
	}
	
	private static void parseIncDec(FunctionTree fn, Node<Tokens> node) throws Exception {
		Node<Tokens> var;
		Node<Tokens> math;
		if(Tokens.peekFront().type == TokenType.PP || Tokens.peekFront().type == TokenType.MM) {
			math = new Node<>(Tokens.popFront());
			var = new Node<>(Tokens.popFront());
		}
		else {
			var = new Node<>(Tokens.popFront());
			math = new Node<>(Tokens.popFront());
			math.addChild(new Node<Tokens>(new Tokens(TokenType.ROOT)));
		}
		math.addChild(var);
		
		node.addChild(math);
		int varOffset = -1;
		for(int i = 0; i < fn.argsList.size(); i++) {
			Symbol s = fn.argsList.get(i);
			if(s.name.equals(var.val.sym.name)) {
				varOffset = Registers.preserveRegisters.length + fn.frameVar.size() + 1 + i;
				break;
			}
		}
		
		if(varOffset < 0)
			for(int i = 0; i < fn.frameVar.size(); i++) {
				Symbol s = fn.frameVar.get(i);
				if(s.name.equals(var.val.sym.name)) {
					varOffset = i;
					break;
				}
			}
		
		if(varOffset < 0) throw new ParserError("Unkown symbol: " + var.val.sym.name);
		var.val.val = varOffset;
	}
	
	private static void parseMathExp(FunctionTree fn, Node<Tokens> node) throws Exception {
		Deque<Node<Tokens>> stack = new LinkedList<Node<Tokens>>();
		Deque<Node<Tokens>> output = new LinkedList<Node<Tokens>>();
		Node<Tokens> nodeTest = new Node<Tokens>(new Tokens(TokenType.LPAR));
		TokenType last = TokenType.ROOT;
		
		while(true) {
			Tokens t = Tokens.popFront();
			if(t.type == TokenType.SC || t.type == TokenType.COMMA || (t.type == TokenType.RPAR && !stack.contains(nodeTest))) {
				Tokens.pushFront(t);
				break;
			}
			
			if(t.type == TokenType.MIN && (Tokens.isMathToken(last) || last == TokenType.LPAR || last == TokenType.ROOT)) {
				t.type = TokenType.NEG;
			}
			if((t.type == TokenType.NUM || t.type == TokenType.SYM || t.type == TokenType.CALL) && (last == TokenType.NUM || last == TokenType.SYM || last == TokenType.CALL || last == TokenType.RPAR)) {
				if(t.type == TokenType.NUM && t.val < 0) {
					Tokens.pushFront(t);
					t = new Tokens(TokenType.PLUS);
				}
				else throw new ParserError("expected math operation after literal type");
			}
			if(Tokens.isMathToken(t.type) && Tokens.isMathToken(last) && Tokens.mathArgCount(t.type) > 1)
				throw new ParserError("expected literal after math operation");
			last = Tokens.mathArgCount(t.type) == 1 ? TokenType.NUM : t.type;
			
			if(t.type == TokenType.NUM) {
				output.add(new Node<Tokens>(t));
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
					if(Tokens.peekFront().type == TokenType.PP || Tokens.peekFront().type == TokenType.MM) {
						Node<Tokens> n =  new Node<Tokens>(new Tokens(TokenType.ROOT));
						Tokens.pushFront(t);
						parseIncDec(fn, n);
						output.add(n.children.get(0));
					} 
					else {
						t.val = varOffset;
						output.add(new Node<Tokens>(t));
					}
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
				
				Node<Tokens> n = new Node<Tokens>(new Tokens(TokenType.ROOT));
				Tokens.pushFront(t);
				parseFunctionCall(fn, callee, n);
				output.add(n.children.get(0));
			}
			else if(t.type == TokenType.PP || t.type == TokenType.MM) {
				Tokens.pushFront(t);
				Node<Tokens> n = new Node<>(new Tokens(TokenType.ROOT));
				parseIncDec(fn, n);
				output.add(n.children.get(0));
			}
			else if(Tokens.mathArgCount(t.type) == 1) {
				Node<Tokens> n = new Node<>(t);
				
				if(Tokens.peekFront().type == TokenType.LPAR) {
					Tokens.popFront();
					parseMathExp(fn, n);
					popToken(TokenType.RPAR, "Missing \')\' in math expression");
				}
				else if(Tokens.peekFront().type == TokenType.SYM) {
					if(Parser.isFunctionCall(Tokens.peekFront().sym)) {
						FunctionTree callee = null;
						for(FunctionTree fns : program) {
							if(fns.name.name.equals(t.sym.name)) {
								callee = fns;
								break;
							}
						}
						
						Parser.parseFunctionCall(fn, callee, nodeTest);
					}
					else if(Parser.isVariable(fn, Tokens.peekFront().sym)) {
						n.children.add(new Node<>(Tokens.popFront()));
					}
					else throw new ParserError("Unkown symbol: " + Tokens.peekFront().sym.name);
				}
				else if(Tokens.peekFront().type == TokenType.NUM) {
					n.children.add(new Node<>(Tokens.popFront()));
				}
				else throw new ParserError("Expected symbol or number literal after single argument math operation");
				output.add(n);
			}
			else if(t.type == TokenType.LPAR) {
				stack.add(new Node<Tokens>(t));
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
				if(stack.isEmpty() || Tokens.evaluationLevel(t.type) > Tokens.evaluationLevel(stack.peekLast().val.type)) {
					stack.add(new Node<Tokens>(t));
				}
				else {
					while(!stack.isEmpty() && Tokens.evaluationLevel(t.type) <= Tokens.evaluationLevel(stack.peekLast().val.type)) {
						output.add(stack.pollLast());
					}
					stack.add(new Node<Tokens>(t));
				}
			}
		}
		
		while(!stack.isEmpty()) output.add(stack.pollLast());
		buildMathTree(output, node);
	}
	
	private static Tokens popToken(TokenType type, String errorMsg) throws Exception {
		Tokens t = Tokens.popFront();
		if(t == null) throw new ParserError(errorMsg);
		if(t.type != type) throw new ParserError(errorMsg);
		return t;
	}
	
	private static void buildMathTree(Deque<Node<Tokens>> stack, Node<Tokens> node) throws Exception {
		if(stack.isEmpty()) return;
		
		node.addChild(stack.pollLast());
		node = node.children.get(node.children.size()-1);
		if(Tokens.isMathToken(node.val.type)) {
			buildMathTree(stack, node);
			buildMathTree(stack, node);
		}
	}
	
	private static boolean isFunctionCall(Symbol sym) {
		for(FunctionTree fn : program) {
			if(fn.name.name.equals(sym.name)) return true;
		}
		
		return false;
	}
	
	private static boolean isVariable(FunctionTree fn, Symbol sym) {
		for(Symbol fn_sym : fn.argsList) {
			if(fn_sym.name.equals(sym.name)) return true;
		}
		
		for(Symbol fn_sym : fn.frameVar) {
			if(fn_sym.name.equals(sym.name)) return true;
		}
		
		return false;
	}
	
	private static void addBuiltInFunctions() {
		program.add(BuiltInFunction.printFnTree());
	}
	
	public static void printParserTree() {
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
