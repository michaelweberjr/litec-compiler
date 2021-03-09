package parser;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Stack;

import common.defs.Registers;
import common.defs.FunctionTree;
import common.defs.Tokens;
import common.defs.TokenType;
import common.util.Node;
import common.util.Pair;

public class CodeGenerator {
	private static int memoryWidth = 8;
	private static int saveOffset;
	private static int label = 0;
	private static Stack<Pair<String, String>> loopStack = new Stack<>();
	
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
			
			for(Node<Tokens> node : fn.statements.root.children) {
				generateStatement(file, fn, node);
				file.write("\n");
			}
			
			if(!fn.hasReturn) generateRet(file, fn, null);
			file.write("\n");
		}
	}

	private static void generateStatement(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {
		if(Tokens.isOpEq(node.val.type)) {
			generateAssignment(file, fn, node);
			return;
		}
		
		switch(node.val.type) {
		case IF:
			generateIf(file, fn, node);
			break;
		case FOR:
			generateFor(file, fn, node);
			break;
		case BREAK:
			generateBreak(file, fn, node);
			break;
		case CONT:
			generateContinue(file, fn, node);
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

	private static void generateIf(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {
			int count = 1;
			for(Node<Tokens> n : node.children) {
				if(n.val.type == TokenType.ELIF || n.val.type == TokenType.ELSE) {
					count++;
				}
			}
			
			String[] jmpPoints = new String[count];
			for(int i = 0; i < count; i++) jmpPoints[i] = "label" + label++;
			
			if(Tokens.isBoolToken(node.children.get(0).val.type))
				generateBool(file, fn, node.children.get(0));
			else if(Tokens.isCmpToken(node.children.get(0).val.type))
				generateCmp(file, fn, node.children.get(0), null, null);
			else generateMath(file, fn, node.children.get(0));
			
			int jmp = 0;
			file.write("\txor\trax, rax\n");
			file.write("\tcmp\trbx, rax\n");
			file.write("\tje " + jmpPoints[jmp] + "\n");

			int index = 1;
			for(; index < node.children.size(); index++) {
				if(node.children.get(index).val.type == TokenType.ELIF || node.children.get(index).val.type == TokenType.ELSE) break;
				
				generateStatement(file, fn, node.children.get(index));
				file.write("\tjmp\t" + jmpPoints[count-1] + "\n");
			}
			
			while(index != node.children.size()) {
				file.write(jmpPoints[jmp++] + ":\n");
				Node<Tokens> n = node.children.get(index++);
				
				if(n.val.type == TokenType.ELIF) {
					if(Tokens.isBoolToken(n.children.get(0).val.type))
						generateBool(file, fn, n.children.get(0));
					else if(Tokens.isCmpToken(n.children.get(0).val.type))
						generateCmp(file, fn, n.children.get(0), null, null);
					else generateMath(file, fn, n.children.get(0));
					
					file.write("\txor\trax, rax\n");
					file.write("\tcmp\trbx, rax\n");
					file.write("\tje " + jmpPoints[jmp] + "\n");
				}
				
				int j = n.val.type == TokenType.ELIF ? 1 : 0;
				for(; j < n.children.size(); j++)					
					generateStatement(file, fn, n.children.get(j));
				
				if(n.val.type == TokenType.ELIF) file.write("\tjmp\t" + jmpPoints[count-1] + "\n");
			}
			
			file.write(jmpPoints[count-1] + ":\n");
	}

	private static void generateFor(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {
		int count = 2 + (node.children.get(2).val.type == TokenType.ROOT ? 0 : 1);
		String[] jmpPoints = new String[count];
		for(int i = 0; i < count; i++) jmpPoints[i] = "label" + label++;
		Pair<String, String> save = new Pair<>(jmpPoints[count-2], jmpPoints[count-1]);
		loopStack.add(save);
		
		if(node.children.get(0).val.type != TokenType.ROOT) generateStatement(file, fn, node.children.get(0));
		
		int jmp = 0;
		file.write(jmpPoints[jmp++] + ":\n");
		
		if(node.children.get(1).val.type != TokenType.ROOT) {
			if(Tokens.isBoolToken(node.children.get(1).val.type))
				generateBool(file, fn, node.children.get(1));
			else if(Tokens.isCmpToken(node.children.get(1).val.type))
				generateCmp(file, fn, node.children.get(1), null, null);
			else generateMath(file, fn, node.children.get(1));
			
			file.write("\txor\trax, rax\n");
			file.write("\tcmp\trbx, rax\n");
			file.write("\tje " + jmpPoints[count - 1] + "\n");
		}
		
		for(int i = 3; i < node.children.size(); i++) 
			generateStatement(file, fn, node.children.get(i));

		if(node.children.get(2).val.type != TokenType.ROOT) {
			file.write(jmpPoints[jmp++] + ":\n");
			generateStatement(file, fn, node.children.get(2));
		}
		
		file.write("\tjmp\t " + jmpPoints[0] + "\n");
		file.write(jmpPoints[jmp] + ":\n");
				
		loopStack.pop();
	}

	private static void generateBreak(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {
		String dest = loopStack.peek().second;
		file.write("\tjmp\t" + dest + "\n");
	}

	private static void generateContinue(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {
		String dest = loopStack.peek().first;
		file.write("\tjmp\t" + dest + "\n");
	}

	private static void generateAssignment(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {
		String src = "";
		String dest = getLocation(node.children.get(0).val, 0);
		
		if(node.children.get(1).val.type == TokenType.CALL) {
			generateCall(file, fn, node.children.get(1));
			src = "rax\n";
		}
		else if(node.children.get(1).val.type == TokenType.NUM) {
			src = node.children.get(1).val.val + "\n";
		}
		else if(node.children.get(1).val.type == TokenType.SYM) {
			src = getLocation(node.children.get(1).val, 0) + "\n";
		}
		else if(Tokens.isBoolToken(node.children.get(1).val.type)) {
			generateBool(file, fn, node.children.get(1));
			src = "rbx\n";
		}
		else if(Tokens.isCmpToken(node.children.get(1).val.type)) {
			generateCmp(file, fn, node.children.get(1), null, null);
			src = "rbx\n";
		}
		else { 
			generateMath(file, fn, node.children.get(1));
			src = "rbx\n";
		}
		
		switch(node.val.type) {
		case ASGN:
			file.write("\tmov\t" + dest + ", " + src);
			break;
		case PLUSEQ:
			file.write("\tmov\t" + dest + ", " + src);
			break;
		case MINEQ:
			file.write("\tmov\t" + dest + ", " + src);
			break;
		case MULEQ:
			file.write("\tmov\trax, " + dest + "\n");
			file.write("\tpush\t" + src);
			file.write("\timul\tqword [rsp]\n");
			file.write("\tadd\trsp, 8\n");
			file.write("\tmov\t" + dest + ", rax\n");
			break;
		case DIVEQ:
			file.write("\tmov\trax, " + dest + "\n");
			file.write("\tpush\t" + src);
			file.write("\tdiv\tqword [rsp]\n");
			file.write("\tadd\trsp, 8\n");
			file.write("\tmov\t" + dest + ", rax\n");
			break;
		case MODEQ:
			file.write("\tmov\trax, " + dest + "\n");
			file.write("\tpush\t" + src);
			file.write("\tdiv\tqword [rsp]\n");
			file.write("\tadd\trsp, 8\n");
			file.write("\tmov\t" + dest + ", rdx\n");
			break;
		case ANDEQ:
			file.write("\tand\t" + dest + ", " + src);
			break;
		case OREQ:
			file.write("\tor\t" + dest + ", " + src);
			break;
		case XOREQ:
			file.write("\txor\t" + dest + ", " + src);
			break;
		case SHLEQ:
			file.write("\tshl\t" + dest + ", " + src);
			break;
		case SHREQ:
			file.write("\tshr\t" + dest + ", " + src);
			break;
		default:
			throw new Exception("Bad token");
		}
	}
	
	private static void generateCall(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {
		
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
			else if(Tokens.isBoolToken(node.children.get(i).val.type)) {
				generateBool(file, fn, node.children.get(i));
				file.write("\tpush\trbx\n");
			}
			else if(Tokens.isCmpToken(node.children.get(i).val.type)) {
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
	
	private static void generateRet(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {
		if(node != null && node.children.size() > 0) {
			generateMath(file, fn, node.children.get(0));
			file.write("\tmov\trax, rbx\n");
		}
		file.write("\tadd\trsp, " + ((Registers.preserveRegisters.length + fn.frameVar.size()) * memoryWidth) + "\n");
		file.write("\tret\n");
	}
	
	private static void generateBool(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {	
		if(node.val.type == TokenType.NOT) {
			if(Tokens.isCmpToken(node.children.get(0).val.type)) {
				node.children.get(0).val.type = Tokens.reverseCmp(node.children.get(0).val.type);
				generateCmp(file, fn, node.children.get(0), null, null);
			}
			else {
				if(Tokens.isBoolToken(node.children.get(0).val.type)) {
					generateBool(file, fn, node.children.get(0));
				}
				else {
					generateMath(file, fn, node.children.get(0));
				}
				
				file.write("\txor\trax, rax\n");
				file.write("\tcmp\trbx, rax\n");
				file.write("\tje label" + label++ + "\n");
				file.write("\txor\trbx, rbx\n");
				file.write("\tjmp label" + label++ + "\n");
				file.write("label" + (label-2) + ":\n");
				file.write("\tmov\tbl, 1\n");
				file.write("label" + (label-1) + ":\n");
			}
		}
		else if(node.val.type == TokenType.AND) {
			String success1 = "label" + label++;
			String success2 = "label" + label++;
			String end = "label" + label++;
			
			if(Tokens.isCmpToken(node.children.get(0).val.type)) {
				generateCmp(file, fn, node.children.get(0), success1, end);
			}
			else {
				if(Tokens.isBoolToken(node.children.get(0).val.type)) {
					generateBool(file, fn, node.children.get(0));
				}
				else {
					generateMath(file, fn, node.children.get(0));
				}
				
				file.write("\txor\trax, rax\n");
				file.write("\tcmp\trbx, rax\n");
				file.write("\tjne\t" + success1 + "\n");
				file.write("\txor\trbx, rbx\n");
				file.write("\tjmp " + end + "\n");
				file.write(success1 + ":\n");
			}
			
			if(Tokens.isCmpToken(node.children.get(1).val.type)) {
				generateCmp(file, fn, node.children.get(1), success2, end);
			}
			else {
				if(Tokens.isBoolToken(node.children.get(1).val.type)) {
					generateBool(file, fn, node.children.get(1));
				}
				else {
					generateMath(file, fn, node.children.get(1));
				}
				
				file.write("\txor\trax, rax\n");
				file.write("\tcmp\trbx, rax\n");
				file.write("\tjne " + success2 + "\n");
				file.write("\txor\trbx, rbx\n");
				file.write("\tjmp\t" + end + "\n");
				file.write(success2 + ":\n");
				file.write("\tmov\tbl, 1\n");
				file.write(end + ":\n");
			}
		}
		else {
			String success = "label" + label++;
			String end = "label" + label++;
			
			if(Tokens.isCmpToken(node.children.get(0).val.type)) {
				node.children.get(0).val.type = Tokens.reverseCmp(node.children.get(0).val.type);
				generateCmp(file, fn, node.children.get(0), success, end);
			}
			else {
				if(Tokens.isBoolToken(node.children.get(0).val.type)) {
					generateBool(file, fn, node.children.get(0));
				}
				else {
					generateMath(file, fn, node.children.get(0));
				}
				
				file.write("\txor\trax, rax\n");
				file.write("\tcmp\trbx, rax\n");
				file.write("\tjne\t" + success + "\n");
			}
			
			if(Tokens.isCmpToken(node.children.get(1).val.type)) {
				node.children.get(1).val.type = Tokens.reverseCmp(node.children.get(1).val.type);
				generateCmp(file, fn, node.children.get(1), success, end);
			}
			else {
				if(Tokens.isBoolToken(node.children.get(1).val.type)) {
					generateBool(file, fn, node.children.get(1));
				}
				else {
					generateMath(file, fn, node.children.get(1));
				}
				
				file.write("\txor\trax, rax\n");
				file.write("\tcmp\trbx, rax\n");
				file.write("\tjne\t" + success + "\n");
				file.write("\txor\trbx, rbx\n");
				file.write("\tjmp\t" + end + "\n");
				file.write(success + ":\n");
				file.write("\tmov\tbl, 1\n");
				file.write(end + ":\n");
			}
		}
	}
	
	private static void generateCmp(FileWriter file, FunctionTree fn, Node<Tokens> node, String success, String end) throws Exception {
		if(success == null) {
			success = "label" + label++;
			end = "label" + label++;
		}
		
		String src = "";
		if(node.children.get(0).val.type == TokenType.CALL) {
			file.write("\tpush\trbx\n");
			generateCall(file, fn, node.children.get(0));
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
			if(Tokens.isBoolToken(node.children.get(0).val.type))
				generateBool(file, fn, node.children.get(0));
			else if(Tokens.isCmpToken(node.children.get(0).val.type))
				generateCmp(file, fn, node.children.get(0), null, null);
			else generateMath(file, fn, node.children.get(0));
			file.write("\tmov\tr13, rbx\n");
			file.write("\tmov\trbx, r12\n");
			src = "r13\n";
		}
		
		if(node.children.get(1).val.type == TokenType.CALL) {
			file.write("\tpush\trbx\n");
			generateCall(file, fn, node.children.get(1));
			file.write("\tpop\trbx\n");
			file.write("\tmov\trbx, rax\n");
		}
		else if(node.children.get(1).val.type == TokenType.NUM) {
			file.write("\tmov\trbx, " + node.children.get(1).val.val + "\n");
		}
		else if(node.children.get(1).val.type == TokenType.SYM) {
			file.write("\tmov\trbx, " + getLocation(node.children.get(1).val, 0) + "\n");
		}
		else {
			if(Tokens.isBoolToken(node.children.get(1).val.type))
				generateBool(file, fn, node.children.get(1));
			else if(Tokens.isCmpToken(node.children.get(1).val.type))
				generateCmp(file, fn, node.children.get(1), null, null);
			else generateMath(file, fn, node.children.get(1));
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
	
	private static void generateMath(FileWriter file, FunctionTree fn, Node<Tokens> node) throws Exception {
		String src = "";
		
		if(node.val.type == TokenType.NUM) {
			file.write("\tmov\trbx, " + node.val.val + "\n");
			return;
		}
		else if (node.val.type == TokenType.SYM) {
			file.write("\tmov\trbx, " + getLocation(node.val, 0) + "\n");
			return;
		}
		
		if(node.children != null) {
			if(Tokens.isIncDec(node.val.type)) {
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
						generateCall(file, fn, node.children.get(1));
						file.write("\tmov\trbx, rax\n");
					}
					else {
						if(Tokens.isBoolToken(node.children.get(1).val.type))
							generateBool(file, fn, node.children.get(1));
						if(Tokens.isCmpToken(node.children.get(1).val.type))
							generateCmp(file, fn, node.children.get(1), null, null);
						else generateMath(file, fn, node.children.get(1));
					}
				}

				if(node.children.get(0).val.type == TokenType.CALL) {
					file.write("\tpush\trbx\n");
					generateCall(file, fn, node.children.get(0));
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
					if(Tokens.isBoolToken(node.children.get(0).val.type))
						generateBool(file, fn, node.children.get(0));
					if(Tokens.isCmpToken(node.children.get(0).val.type))
						generateCmp(file, fn, node.children.get(0), null, null);
					else generateMath(file, fn, node.children.get(0));
					
					file.write("\tmov\tr13, rbx\n");
					file.write("\tmov\trbx, r12\n");
					src = "r13\n";
				}
			}
		}
		
		switch(node.val.type) {
		case BNOT:
			file.write("\tmov\trbx, " + src);
			file.write("\tnot\trbx\n");
			break;
		case NEG:
			file.write("\tmov\trbx, " + src);
			file.write("\tneg\trbx\n");
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
			file.write("\tadd\trsp, 8\n");
			file.write("\tmov\trbx, rax\n");
			break;
		case DIV:
		case MOD:
			file.write("\tmov\trax,rbx\n");
			file.write("\txor\trdx, rdx\n");
			file.write("\tpush\t" + src);
			file.write("\tdiv\tqword [rsp]\n");
			file.write("\tadd\trsp, 8\n");
			if(node.val.type == TokenType.DIV) file.write("\tmov\trbx, rax\n");
			else file.write("\tmov\trbx, rdx\n");
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
	
	private static String getLocation(Tokens t, int stackOffset) throws Exception {
		if(t.val >= saveOffset) {
			int offset = t.val - (saveOffset + Registers.preserveRegisters.length + 1);
			if(offset >= 0 && offset < Registers.stackRegisters.length) return Registers.stackRegisters[offset];
			else return "qword [rsp" + ((offset == 0 && stackOffset == 0) ? "]" : ("+" + ((stackOffset + offset) * memoryWidth) + "]"));
		}
		else return "qword [rsp" + ((t.val == 0 && stackOffset == 0) ? "]" : ("+" + ((stackOffset + t.val) * memoryWidth) + "]"));
	}
}
