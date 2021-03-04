package parser;

import java.io.FileWriter;
import java.util.ArrayList;

import common.defs.Registers;
import common.defs.FunctionTree;
import common.defs.Token;
import common.defs.TokenType;
import common.util.Node;

public class CodeGenerator {
	private static int memoryWidth = 8;
	private static int saveOffset;
	private static int label = 0;
	
	public static void generate(FileWriter file, ArrayList<FunctionTree> program) throws Exception {	
		file.write("global main\n\n");
		
		for(FunctionTree fn : program) {
			if(fn.isBuiltIn) {
				file.write(fn.builtInCode);
				continue;
			}
			
			file.write(fn.name.name + ":\n");
			file.write("\tsub\trsp, " + ((fn.frameVar.size() + Registers.preserveRegisters.length) * memoryWidth) + "\n\n");
			saveOffset = fn.frameVar.size();
			
			for(Node<Token> node : fn.statements.root.children) {
				generateStatement(file, fn, node);
				file.write("\n");
			}
			
			if(!fn.hasReturn) generateRet(file, fn, null);
			file.write("\n");
		}
	}

	private static void generateStatement(FileWriter file, FunctionTree fn, Node<Token> node) throws Exception {
		switch(node.val.type) {
		case ASGN:
			generateAssignment(file, fn, node);
			break;
		case CALL:
			generateCall(file, fn, node);
			break;
		case RET:
			generateRet(file, fn, node);
			break;
		case PP:
		case MM:
			generateMath(file, fn, node);
			break;
		default:
			throw new Exception("Bad root node type: " + node.val.type.toString());
		}
	}

	private static void generateAssignment(FileWriter file, FunctionTree fn, Node<Token> node) throws Exception {
		if(node.children.get(1).val.type == TokenType.CALL) {
			generateCall(file, fn, node.children.get(1));
			file.write("\tmov\t" + getLocation(node.children.get(0).val, 0) + ", rax\n");
		}
		else if(node.children.get(1).val.type == TokenType.NUM) {
			file.write("\tmov\t" + getLocation(node.children.get(0).val, 0) + ", " + node.children.get(1).val.val + "\n");
		}
		else if(node.children.get(1).val.type == TokenType.SYM) {
			file.write("\tmov\t" + getLocation(node.children.get(0).val, 0) + ", " + getLocation(node.children.get(1).val, 0) + "\n");
		}
		else if(Token.isBoolToken(node.children.get(1).val.type)) {
			generateBool(file, fn, node.children.get(1));
			file.write("\tpush\trbx\n");
		}
		else if(Token.isCmpToken(node.children.get(1).val.type)) {
			generateCmp(file, fn, node.children.get(1), null, null);
			file.write("\tpush\trbx\n");
		}
		else { 
			generateMath(file, fn, node.children.get(1));
			file.write("\tmov\t" + getLocation(node.children.get(0).val, 0) + ", rbx\n");
		}
	}
	
	private static void generateCall(FileWriter file, FunctionTree fn, Node<Token> node) throws Exception {
		
		for(int i = node.children.size() - 1; i > 0; i--) {
			int stackOffset = node.children.size() - i - 1;
			if(node.children.get(i).val.type == TokenType.NUM) {
				file.write("\tsub\trsp, 8\n");
				file.write("\tmov\tqword [rsp], " + node.children.get(i).val.val + "\n");
			}
			else if(node.children.get(i).val.type == TokenType.SYM) {		
				file.write("\tpush\t" + getLocation(node.children.get(i).val, stackOffset) + "\n");
			}
			else if(node.children.get(i).val.type == TokenType.CALL) {
				generateCall(file, fn, node.children.get(i));
				file.write("\tpush\trax\n");
			}
			else if(Token.isBoolToken(node.children.get(i).val.type)) {
				generateBool(file, fn, node.children.get(i));
				file.write("\tpush\trbx\n");
			}
			else if(Token.isCmpToken(node.children.get(i).val.type)) {
				generateCmp(file, fn, node.children.get(i), null, null);
				file.write("\tpush\trbx\n");
			}
			else {
				generateMath(file, fn, node.children.get(i));
				file.write("\tpush\trbx\n");
			}
		}
		
		for(int i = 0; i < Registers.preserveRegisters.length; i++) {
			file.write("\tmov\tqword [rsp+" + ((saveOffset + (node.children.size() - 1) + i) * memoryWidth) 
						+ "], " + Registers.preserveRegisters[i] + "\n");
		}
		
		for(int i = 1; i < node.children.size(); i++) {
			if(i <= Registers.stackRegisters.length) {
				file.write("\tmov\t" + Registers.stackRegisters[i-1] + ", [rsp+" + ((i-1) * memoryWidth) + "]\n");
			}
			else break;
		}
		
		file.write("\tcall\t" + node.children.get(0).val.sym.name + "\n");
		file.write("\tadd\trsp, " + ((node.children.size() - 1) * memoryWidth) + "\n");
		
		for(int i = 0; i < Registers.preserveRegisters.length; i++) {
			file.write("\tmov\t" + Registers.preserveRegisters[i] + ", [rsp+" + ((saveOffset + i) * memoryWidth) + "]\n");
		}
	}
	
	private static void generateRet(FileWriter file, FunctionTree fn, Node<Token> node) throws Exception {
		if(node != null && node.children.size() > 0) {
			generateMath(file, fn, node.children.get(0));
			file.write("\tmov\trax, rbx\n");
		}
		file.write("\tadd\trsp, " + ((Registers.preserveRegisters.length + fn.frameVar.size()) * memoryWidth) + "\n");
		file.write("\tret\n");
	}
	
	private static void generateBool(FileWriter file, FunctionTree fn, Node<Token> node) throws Exception {	
		if(node.val.type == TokenType.NOT) {
			if(Token.isCmpToken(node.children.get(0).val.type)) {
				node.children.get(0).val.type = Token.reverseCmp(node.children.get(0).val.type);
				generateCmp(file, fn, node.children.get(0), null, null);
			}
			else {
				if(Token.isBoolToken(node.children.get(0).val.type)) {
					generateBool(file, fn, node.children.get(0));
				}
				else {
					generateMath(file, fn, node.children.get(0));
				}
				
				file.write("\txor\trax, rax\n");
				file.write("\tcmp\nrbx, rax\n");
				file.write("\tje label" + label++ + "\n");
				file.write("\txor\trbx, rbx\n");
				file.write("\tjmp " + label++ + "\n");
				file.write("label" + (label-2) + ":\n");
				file.write("\tmov\tbl, 1\n");
				file.write("label" + (label-1) + ":\n");
			}
		}
		else if(node.val.type == TokenType.AND) {
			String success1 = "label" + label++;
			String success2 = "label" + label++;
			String end = "label" + label++;
			
			if(Token.isCmpToken(node.children.get(0).val.type)) {
				generateCmp(file, fn, node.children.get(0), success1, end);
			}
			else {
				if(Token.isBoolToken(node.children.get(0).val.type)) {
					generateBool(file, fn, node.children.get(0));
				}
				else {
					generateMath(file, fn, node.children.get(0));
				}
				
				file.write("\txor\trax, rax\n");
				file.write("\tcmp\nrbx, rax\n");
				file.write("\tje label" + success1 + "\n");
				file.write("\txor\trbx, rbx\n");
				file.write("jmp " + end + "\n");
				file.write("label" + success1 + ":\n");
			}
			
			if(Token.isCmpToken(node.children.get(1).val.type)) {
				generateCmp(file, fn, node.children.get(1), success2, end);
			}
			else {
				if(Token.isBoolToken(node.children.get(1).val.type)) {
					generateBool(file, fn, node.children.get(1));
				}
				else {
					generateMath(file, fn, node.children.get(1));
				}
				
				file.write("\txor\trax, rx\n");
				file.write("\tcmp\nrbx, rax\n");
				file.write("\tje label" + success2 + "\n");
				file.write("\txor\trbx, rbx\n");
				file.write("\tjmp " + end + "\n");
				file.write("label" + success2 + ":\n");
				file.write("\tmov\tbl, 1\n");
				file.write("label" + end + ":\n");
			}
		}
		else {
			String success1 = "label" + label++;
			String success2 = "label" + label++;
			String end = "label" + label++;
			
			if(Token.isCmpToken(node.children.get(0).val.type)) {
				node.children.get(0).val.type = Token.reverseCmp(node.children.get(0).val.type);
				generateCmp(file, fn, node.children.get(0), success1, end);
			}
			else {
				if(Token.isBoolToken(node.children.get(0).val.type)) {
					generateBool(file, fn, node.children.get(0));
				}
				else {
					generateMath(file, fn, node.children.get(0));
				}
				
				file.write("\txor\trax, rax\n");
				file.write("\tcmp\nrbx, rax\n");
				file.write("\tjne label" + success1 + "\n");
				file.write("\txor\trbx, rbx\n");
				file.write("jmp " + end + "\n");
				file.write("label" + success1 + ":\n");
			}
			
			if(Token.isCmpToken(node.children.get(1).val.type)) {
				node.children.get(0).val.type = Token.reverseCmp(node.children.get(0).val.type);
				generateCmp(file, fn, node.children.get(1), success2, end);
			}
			else {
				if(Token.isBoolToken(node.children.get(1).val.type)) {
					generateBool(file, fn, node.children.get(1));
				}
				else {
					generateMath(file, fn, node.children.get(1));
				}
				
				file.write("\txor\trax, rx\n");
				file.write("\tcmp\nrbx, rax\n");
				file.write("\tjne label" + success2 + "\n");
				file.write("\txor\trbx, rbx\n");
				file.write("\tjmp " + end + "\n");
				file.write("label" + success2 + ":\n");
				file.write("\tmov\tbl, 1\n");
				file.write("label" + end + ":\n");
			}
		}
	}
	
	private static void generateCmp(FileWriter file, FunctionTree fn, Node<Token> node, String success, String end) throws Exception {
		if(success == null) {
			success = "label" + label++;
			end = "label" + label++;
		}
		
		String src = "";
		if(node.children.get(0).val.type == TokenType.CALL) {
			file.write("\tpush\trbx\n");
			generateCall(file, fn, node);
			file.write("\tpop\trbx\n");
			src = "rax\n";			
		}
		else if(node.children.get(0).val.type == TokenType.NUM) {
			src = node.children.get(0).val.val + "\n";
		}
		else if(node.children.get(0).val.type == TokenType.SYM) {
			src = getLocation(node.children.get(0).val, 0) + "\n";
		}
		else {
			file.write("\tmov\tr12, rbx\n");
			generateMath(file, fn, node.children.get(0));
			file.write("\tmov\tr13, rbx\n");
			file.write("\tmov\trbx, r12\n");
			src = "r13\n";
		}
		
		String op = "";
		switch(node.val.type) {
		case EQ:
			op = "je";
			break;
		case NE:
			op = "jne";
			break;
		case LESS:
			op = "jl";
			break;
		case LE:
			op = "jle";
			break;
		case GRT:
			op = "jg";
			break;
		case GE:
			op = "jge";
			break;
		default:
			throw new Exception("Bad token");
		}
		
		file.write("\tmov\trax, " + src);
		file.write("\tcmp\trbx, rax\n");
		file.write("\t" + op + "\t" + success + "\n");
		file.write("\txor\trbx, rbx\n");
		file.write("\tjmp\t" + end + "\n");
		file.write(success + ":\n");
		file.write("\tmov\tbl, 1\n");
		file.write(end + ":\n");
	}
	
	private static void generateMath(FileWriter file, FunctionTree fn, Node<Token> node) throws Exception {
		String src = "";
		
		if(node.children != null) {
			if(Token.isIncDec(node.val.type)) {
				String op = node.val.type == TokenType.PP ? "\tinc\t" : "\tdec\t";
				
				if(node.children.size() == 1) {
					String dest = getLocation(node.children.get(0).val, 0) + "\n";
					file.write(op + dest);
					file.write("\tmov\trbx, " + dest);
				}
				else {
					String dest = getLocation(node.children.get(1).val, 0) + "\n";
					file.write("\tmov\trbx, " + dest);
					file.write(op + dest);
				}
				return;
			}
			else {
				if(node.children.size() == 2) {
					if(node.children.get(1).val.type == TokenType.NUM) {
						file.write("\tmov\trbx, " + node.children.get(1).val.val + "\n");
					}
					else if(node.children.get(1).val.type == TokenType.SYM) {
						file.write("\tmov\trbx, " + getLocation(node.children.get(1).val, 0) + "\n");
					}
					else if(node.children.get(1).val.type == TokenType.CALL) {
						generateCall(file, fn, node);
						file.write("\tmov\trbx, rax\n");
					}
					else {
						generateMath(file, fn, node.children.get(1));
					}
				}

				if(node.children.get(0).val.type == TokenType.CALL) {
					file.write("\tpush\trbx\n");
					generateCall(file, fn, node);
					file.write("\tpop\trbx\n");
					src = "rax\n";			
				}
				else if(node.children.get(0).val.type == TokenType.NUM) {
					src = node.children.get(0).val.val + "\n";
				}
				else if(node.children.get(0).val.type == TokenType.SYM) {
					src = getLocation(node.children.get(0).val, 0) + "\n";
				}
				else {
					file.write("\tmov\tr12, rbx\n");
					generateMath(file, fn, node.children.get(0));
					file.write("\tmov\tr13, rbx\n");
					file.write("\tmov\trbx, r12\n");
					src = "r13\n";
				}
			}
		}
		
		switch(node.val.type) {
		case BNOT:
			file.write("\tnot\trbx");
			break;
		case NEG:
			file.write("\tneg\trbx");
			break;
		case PLUS:
			file.write("\tadd\trbx, " + src);
			break;
		case MIN:
			file.write("\tsub\trbx, " + src);
			break;
		case MUL:
			file.write("\tmov\trax,rbx\n");
			file.write("\tpush\t" + src);
			file.write("\timul\tqword [rsp]\n");
			file.write("\tmov\trbx, rax\n");
			file.write("\tadd\trsp, " + memoryWidth + "\n");
			break;
		case DIV:
		case MOD:
			file.write("\tmov\trax,rbx\n");
			file.write("\tpush\t" + src);
			file.write("\txor\trdx, rdx\n");
			file.write("\tdiv\tqword [rsp]\n");
			if(node.val.type == TokenType.DIV) file.write("\tmov\trbx, rax\n");
			else file.write("\tmov\trbx, rdx\n");
			file.write("\tadd\trsp, " + memoryWidth + "\n");
			break;
		case BAND:
			file.write("\tand\trbx, " + src);
			break;
		case BOR:
			file.write("\tor\trbx, " + src);
			break;
		case BXOR:
			file.write("\txor\trbx, " + src);
			break;
		case LSH:
			file.write("\tshl\trbx, " + src);
			break;
		case RSH:
			file.write("\tshr\trbx, " + src);
			break;
		case CALL:
			generateCall(file, fn, node);
			file.write("\tmov\trbx, rax\n");
			break;
		case NUM:
			file.write("\tmov\trbx, " + node.val.val + "\n");
			break;
		case SYM:
			file.write("\tmov\trbx, [rsp+" + (node.val.val * memoryWidth) + "]\n" );
			break;
		default:
			throw new Exception("Incorrect math operating in generateMath");
		}
	}
	
	/*
	PP, MM,
	 * 
	 */
	
	private static String getLocation(Token t, int stackOffset) throws Exception {
		if(t.val >= saveOffset) {
			int offset = t.val - (saveOffset + Registers.preserveRegisters.length + 1);
			if(offset >= 0 && offset < Registers.stackRegisters.length) return Registers.stackRegisters[offset];
			else return "qword [rsp" + ((offset == 0 && stackOffset == 0) ? "]" : ("+" + ((stackOffset + offset) * memoryWidth) + "]"));
		}
		else return "qword [rsp" + ((t.val == 0 && stackOffset == 0) ? "]" : ("+" + (stackOffset * memoryWidth) + "]"));
	}
}
