package interpreter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

import common.defs.Instruction;
import common.defs.Location;
import common.defs.OpCode;
import common.defs.Registers;
import common.defs.Symbol;
import common.defs.Tokens;
import common.defs.TokenType;
import common.util.Pair;

public class Tokenizer {

	public static ArrayList<Instruction> parseProgram(String text) throws Exception {
		ArrayList<Instruction> byteCode = new ArrayList<Instruction>();
		Deque<Tokens> tokens = new LinkedList<Tokens>();
		tokenize(tokens, VirtualMachine.preamble);
		tokenize(tokens, text);
		
		HashMap<String, Pair<Integer, ArrayList<Integer>>> labelList = new HashMap<>();
		HashMap<String, Integer> registers = new HashMap<>();
		for(int i = 0; i < Registers.list.length; i++) registers.put(Registers.list[i], i);
		for(int i = 0; i < Registers.lower.length; i++) registers.put(Registers.lower[i], i);
		
		while(!tokens.isEmpty()) {
			Tokens t = tokens.pollFirst();
			if(t.type != TokenType.SYM) throw new Exception("Each line must start with a symbol");
			
			Instruction code = new Instruction(OpCode.NOP);
			boolean isPtr = false;
			if(!tokens.isEmpty() && tokens.peek().type == TokenType.COLON) {
				tokens.pollFirst();
				if(labelList.containsKey(t.sym.name)) {
					Pair<Integer, ArrayList<Integer>> address = labelList.get(t.sym.name);
					address.first = byteCode.size();
					for(Integer i : address.second) byteCode.get(i).dest_val = address.first.longValue();
				}
				else {
					Pair<Integer, ArrayList<Integer>> address = new Pair<>(byteCode.size(), null);
					labelList.put(t.sym.name, address);
				}
			}
			else {
				switch(t.sym.name) {
				// no args ops
				case "nop":
					byteCode.add(new Instruction(OpCode.NOP));
					break;
				case "ext":
					byteCode.add(new Instruction(OpCode.EXT));
					break;
				case "ret":
					byteCode.add(new Instruction(OpCode.RET));
					break;
				// jumping opcodes
				case "call":
				case "jmp":
				case "je":
				case "jne":
				case "jl":
				case "jle":
				case "jg":
				case "jge":
					Tokens fn = tokens.pollFirst();
					if(fn == null || fn.type != TokenType.SYM) throw new Exception("Missing function name");
					
					if(getBuiltIn(fn.sym.name) > -1) {
						code = new Instruction(OpCode.CALLB);
						code.dest = Location.IMED;
						code.dest_val = getBuiltIn(fn.sym.name);
					}
					else {
						code = new Instruction(textToCode(t.sym.name));
						code.dest = Location.IMED;
						if(labelList.containsKey(fn.sym.name)) {
							Pair<Integer, ArrayList<Integer>> address = labelList.get(fn.sym.name);
							if(address.first < 0) address.second.add(byteCode.size());
							else code.dest_val = labelList.get(fn.sym.name).first.longValue();
						}
						else {
							ArrayList<Integer> ref = new ArrayList<>();
							ref.add(byteCode.size());
							Pair<Integer, ArrayList<Integer>> address = new Pair<>(-1, ref);
							labelList.put(fn.sym.name, address);
							labelList.isEmpty();
						}
					}
					
					byteCode.add(code);
					break;
				// single arg general ops
				case "push":
				case "pop":
				case "imul":
				case "div":
				case "inc":
				case "dec":
				case "neg":
				case "not":
					switch(t.sym.name) {
					case "push":
						code = new Instruction(OpCode.PUSH);
						break;
					case "pop":
						code = new Instruction(OpCode.POP);
						break;
					case "imul":
						code = new Instruction(OpCode.IMUL);
						break;
					case "div":
						code = new Instruction(OpCode.DIV);
						break;
					case "inc":
						code = new Instruction(OpCode.INC);
						break;
					case "dec":
						code = new Instruction(OpCode.DEC);
						break;
					case "neg":
						code = new Instruction(OpCode.NEG);
						break;
					case "not":
						code = new Instruction(OpCode.NOT);
						break;
					}
					
					isPtr = false;
					t = tokens.pollFirst();
					if(t == null) throw new Exception("Error: end of file");
					
					if(t.type == TokenType.LBRACKET) {
						isPtr = true;
						t = tokens.pollFirst();
						if(t == null) throw new Exception("Error: end of file");
					}
					
					if(t.type == TokenType.NUM) {
						code.dest = Location.IMED;
						code.dest_val = t.val;
					}
					else if(t.type == TokenType.SYM) {
						if(isPtr) code.dest = Location.REGP;
						else code.dest = Location.REG;
						
						code.dest_val = registers.get(t.sym.name);
						
						if(isPtr) {
							if(tokens.isEmpty()) throw new Exception("Error: end of file");
							if(tokens.peek().type == TokenType.RBRACKET) {
								tokens.pollFirst();
								code.dest_offset = 0;
							}
							else if(tokens.peek().type == TokenType.PLUS) {
								tokens.pollFirst();
								t = tokens.pollFirst();
								if(tokens.isEmpty() || t.type != TokenType.NUM) throw new Exception("Expected number for register offset");
								code.dest_offset = t.val;
								if(tokens.isEmpty() || tokens.peek().type != TokenType.RBRACKET) throw new Exception("Missing right bracket");
								tokens.pollFirst();
							}
							else throw new Exception("Bad token: " + tokens.peek().type);
						}
					}
					else throw new Exception("Unkown type");
					
					byteCode.add(code);
					break;
			// double args ops
			case "mov":
			case "add":
			case "sub":
			case "xor":
			case "or":
			case "and":
			case "shl":
			case "shr":
			case "cmp":
				switch(t.sym.name) {
					case "mov":
						code = new Instruction(OpCode.MOV);
						break;
					case "add":
						code = new Instruction(OpCode.ADD);
						break;
					case "sub":
						code = new Instruction(OpCode.SUB);
						break;
					case "xor":
						code = new Instruction(OpCode.XOR);
						break;
					case "or":
						code = new Instruction(OpCode.OR);
						break;
					case "and":
						code = new Instruction(OpCode.AND);
						break;
					case "shl":
						code = new Instruction(OpCode.SHL);
						break;
					case "shr":
						code = new Instruction(OpCode.SHR);
						break;
					case "cmp":
						code = new Instruction(OpCode.CMP);
						break;
					}
					
					isPtr = false;
					t = tokens.pollFirst();
					if(t == null) throw new Exception("Error: end of file");
					
					if(t.type == TokenType.LBRACKET) {
						isPtr = true;
						t = tokens.pollFirst();
					}
					
					if(t.type == TokenType.SYM) {
						if(isPtr) code.dest = Location.REGP;
						else code.dest = Location.REG;
						
						code.dest_val = registers.get(t.sym.name);
						
						if(isPtr) {
							if(tokens.isEmpty()) throw new Exception("Error: end of file");
							if(tokens.peek().type == TokenType.RBRACKET) {
								tokens.pollFirst();
								code.dest_offset = 0;
							}
							else if(tokens.peek().type == TokenType.PLUS) {
								tokens.pollFirst();
								t = tokens.pollFirst();
								if(tokens.isEmpty() || t.type != TokenType.NUM) throw new Exception("Expected number for register offset");
								code.dest_offset = t.val;
								if(tokens.isEmpty() || tokens.peek().type != TokenType.RBRACKET) throw new Exception("Missing right bracket");
								tokens.pollFirst();
							}
							else throw new Exception("Bad token: " + tokens.peek().type);
						}
					}
					else throw new Exception("Unkown type");
					
					if(tokens.isEmpty() || tokens.peek().type != TokenType.COMMA) throw new Exception("Missing comma between instruction arguments");
					tokens.pollFirst();
					
					isPtr = false;
					t = tokens.pollFirst();
					if(t == null) throw new Exception("Error: end of file");
					if(t.type == TokenType.LBRACKET) {
						isPtr = true;
						t = tokens.pollFirst();
						if(tokens.isEmpty()) throw new Exception("Error: end of file");
					}
					
					if(t.type == TokenType.NUM) {
						code.src = Location.IMED;
						code.src_val = t.val;
					}
					else if(t.type == TokenType.SYM) {
						if(isPtr) code.src = Location.REGP;
						else code.src = Location.REG;
						
						code.src_val = registers.get(t.sym.name);
						
						if(isPtr) {
							if(tokens.isEmpty()) throw new Exception("Error: end of file");
							if(tokens.peek().type == TokenType.RBRACKET) {
								tokens.pollFirst();
								code.src_offset = 0;
							}
							else if(tokens.peek().type == TokenType.PLUS) {
								tokens.pollFirst();
								t = tokens.pollFirst();
								if(tokens.isEmpty() || t.type != TokenType.NUM) throw new Exception("Expected number for register offset");
								code.src_offset = t.val;
								if(tokens.isEmpty() || tokens.peek().type != TokenType.RBRACKET) throw new Exception("Missing right bracket");
								tokens.pollFirst();
							}
							else throw new Exception("Bad token: " + tokens.peek().type);
						}
					}
					else throw new Exception("Unkown type");
					
					byteCode.add(code);
					break;
				}
			}
		}
		
		for(Map.Entry<String, Pair<Integer, ArrayList<Integer>>> entry : labelList.entrySet()) {
			if(entry.getValue().first < 0)
				throw new Exception("Missing label definition: " + entry.getKey());
		}
		
		return byteCode;
	}
	
	private static void tokenize(Deque<Tokens> tokens, String text) throws Exception {
		int start = skipPreamble(text);
		
		for(int i = start; i < text.length(); i++) {
			char ch = text.charAt(i);
			// skip whitespace
			if(ch == ' ' || ch == '\t' || ch =='\r' || ch == '\n') continue;
			
			// skip comments
			if(ch == ';') {
				while(true) { 
					if(text.charAt(i) == '\n') break;
				}
				continue;
			}
			
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
				tokens.add(new Tokens(TokenType.NUM, val));
				i--;
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
				
				if(getBuiltIn(name) > -1 && ch == ':') {
					i = skipBuiltIn(text, i);
					continue;
				}
				
				if(!name.equals("qword"))				
					tokens.add(new Tokens(TokenType.SYM, new Symbol(name)));
				i--;
				
				continue;
			}
			
			// grab the odd single characters
			switch(ch) {
			case ':':
				tokens.add(new Tokens(TokenType.COLON));
				break;
			case '[':
				tokens.add(new Tokens(TokenType.LBRACKET));
				break;
			case ']':
				tokens.add(new Tokens(TokenType.RBRACKET));
				break;
			case ',':
				tokens.add(new Tokens(TokenType.COMMA));
				break;
			case '+':
				tokens.add(new Tokens(TokenType.PLUS));
				break;
			default:
				throw new Exception("Bad token in interpreter: " + ch);
			}
		}	
	}

	private static int skipPreamble(String text) {
		String search = "SECTION .TEXT";
		int res = text.indexOf(search);
		if(res < 0) return 0;
		else return res+ search.length();
	}
	
	private static int skipBuiltIn(String text, int start) {
		String search = "ret";
		int end = text.indexOf(search, start);
		return end + search.length();
	}
	
	private static int getBuiltIn(String name) {
		for(int i = 0; i < VirtualMachine.builtInFnName.length; i++)
			if(VirtualMachine.builtInFnName[i].equals(name))
				return i;
		
		return -1;
	}
	
	static OpCode textToCode(String text) {
		switch(text) {
		case "call":
			return OpCode.CALL;
		case "jmp":
			return OpCode.JMP;
		case "je":
			return OpCode.JE;
		case "jne":
			return OpCode.JNE;
		case "jl":
			return OpCode.JL;
		case "jle":
			return OpCode.JLE;
		case "jg":
			return OpCode.JG;
		case "jge":
			return OpCode.JGE;
		default:
			return OpCode.NOP;
		}
	}
}
