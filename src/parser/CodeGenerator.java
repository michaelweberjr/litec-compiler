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
		case EQ:
			generateAssignment(file, node);
			break;
		case CALL:
			generateCall(file, node);
			break;
		case RET:
			generateRet(file, fn, node);
			break;
		default:
			throw new Exception("Bad root node type: " + node.val.type.toString());
		}
	}

	private static void generateAssignment(FileWriter file, Node<Token> node) throws Exception {
		if(node.children.get(1).val.type == TokenType.CALL) {
			generateCall(file, node.children.get(1));
			file.write("\tmov\tqword [rsp+" + (memoryWidth * node.children.get(0).val.val) + "], rax\n");
		}
		else if(node.children.get(1).val.type == TokenType.NUM) {
			file.write("\tmov\tqword [rsp+" + (memoryWidth * node.children.get(0).val.val) + "], " + node.children.get(1).val.val + "\n");
		}
		else if(node.children.get(1).val.type == TokenType.SYM) {
			file.write("\tmov\tqword [rsp+" + (memoryWidth * node.children.get(0).val.val) + "], [rsp+" + (memoryWidth * node.children.get(1).val.val) + "]\n");
		}
		else { 
			generateMath(file, node.children.get(1));
			file.write("\tmov\tqword [rsp+" + (memoryWidth * node.children.get(0).val.val) + "], rbx\n");
		}
	}
	
	private static void generateCall(FileWriter file, Node<Token> node) throws Exception {
		for(int i = node.children.size() - 1; i > 0; i--) {
			int stackOffset = node.children.size() - i - 1;
			if(node.children.get(i).val.type == TokenType.NUM) {
				file.write("\tsub\trsp, 8\n");
				file.write("\tmov\tqword [rsp], " + node.children.get(i).val.val + "\n");
			}
			else if(node.children.get(i).val.type == TokenType.SYM) {
				file.write("\tpush\tqword [rsp+" + (memoryWidth * (node.children.get(i).val.val + stackOffset)) + "]\n");
			}
			else if(node.children.get(i).val.type == TokenType.CALL) {
				generateCall(file, node.children.get(i));
				file.write("\tpush\trax\n");
			}
			else {
				generateMath(file, node.children.get(i));
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
			generateMath(file, node.children.get(0));
			file.write("\tmov\trax, rbx\n");
		}
		file.write("\tadd\trsp, " + ((Registers.preserveRegisters.length + fn.frameVar.size()) * memoryWidth) + "\n");
		file.write("\tret\n");
	}
	
	private static void generateMath(FileWriter file, Node<Token> node) throws Exception {
		String src = "";
		
		if(node.children != null && node.children.size() == 2) {
			if(node.children.get(1).val.type == TokenType.NUM) {
				file.write("\tmov\trbx, " + node.children.get(1).val.val + "\n");
			}
			else if(node.children.get(1).val.type == TokenType.SYM) {
				file.write("\tmov\trbx, [rsp+" + (node.children.get(1).val.val * memoryWidth) + "]\n");
			}
			else if(node.children.get(1).val.type == TokenType.CALL) {
				generateCall(file, node);
				file.write("\tmov\trbx, rax\n");
			}
			else {
				generateMath(file, node.children.get(1));
			}
			
			if(node.children.get(0).val.type == TokenType.CALL) {
				file.write("\tpush\trbx\n");
				generateCall(file, node);
				file.write("\tpop\trbx\n");
				src = "rax\n";			
			}
			else if(node.children.get(0).val.type == TokenType.NUM) {
				src = node.children.get(0).val.val + "\n";
			}
			else if(node.children.get(0).val.type == TokenType.SYM) {
				src = "qword [rsp+" + (node.children.get(0).val.val * memoryWidth) + "]\n";
			}
			else {
				file.write("\tmov\tr12, rbx\n");
				generateMath(file, node.children.get(0));
				file.write("\tmov\tr13, rbx\n");
				file.write("\tmov\trbx, r12\n");
				src = "r13\n";
			}
		}
		
		switch(node.val.type) {
		case PLUS:
			file.write("\tadd\trbx, " + src);
			break;
		case MIN:
			//file.write("\tmov r12, " + src);
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
		case CALL:
			generateCall(file, node);
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
}
