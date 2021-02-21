package parser;

import java.io.FileWriter;
import java.util.ArrayList;

import parser.defs.FunctionTree;
import parser.defs.Token;
import parser.defs.TokenType;
import parser.util.Node;

public class CodeGenerator {
	private static int memoryWidth = 8;
	
	public static void generate(FileWriter file, ArrayList<FunctionTree> program) throws Exception {	
		file.write("global main\n\n");
		
		for(FunctionTree fn : program) {
			if(fn.isBuiltIn) {
				file.write(fn.builtInCode);
				continue;
			}
			
			file.write(fn.name.name + ":\n");
			if(fn.frameVar.size() > 0) file.write("\tsub\trsp, " + (fn.frameVar.size() * memoryWidth) + "\n\n");
			
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
			file.write("\tmov\t[rsp+" + (memoryWidth * node.children.get(0).val.val) + "], rax\n");
		}
		else if(node.children.get(1).val.type == TokenType.NUM) {
			file.write("\tmov\t[rsp+" + (memoryWidth * node.children.get(0).val.val) + "], " + node.children.get(1).val.val + "\n");
		}
		else if(node.children.get(1).val.type == TokenType.SYM) {
			file.write("\tmov\t[rsp+" + (memoryWidth * node.children.get(0).val.val) + "], [rsp+" + (memoryWidth * node.children.get(1).val.val) + "]\n");
		}
		else { 
			generateMath(file, node.children.get(1));
			file.write("\tmov\t[rsp+" + (memoryWidth * node.children.get(0).val.val) + "], rbx\n");
		}
	}
	
	private static void generateCall(FileWriter file, Node<Token> node) throws Exception {
		for(int i = node.children.size() - 1; i > 0; i--) {
			if(node.children.get(i).val.type == TokenType.NUM) {
				file.write("\tpush\t" + node.children.get(i).val.val + "\n");
			}
			else if(node.children.get(i).val.type == TokenType.SYM) {
				file.write("\tpush\t[rsp+" + (memoryWidth * node.children.get(i).val.val) + "]\n");
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
		file.write("\tcall\t" + node.children.get(0).val.sym.name + "\n");
		file.write("\tadd\trsp, " + ((node.children.size() - 1) * memoryWidth) + "\n");
	}
	
	private static void generateRet(FileWriter file, FunctionTree fn, Node<Token> node) throws Exception {
		if(node != null && node.children.size() > 0) {
			generateMath(file, node.children.get(0));
			file.write("\tmov\trax, rbx\n");
		}
		if(fn.frameVar.size() > 0) file.write("\tadd\trsp, " + (fn.frameVar.size() * memoryWidth) + "\n");
		file.write("\tret\n");
	}
	
	private static void generateMath(FileWriter file, Node<Token> node) throws Exception {
		String src = "";
		
		if(node.children != null && node.children.size() >= 2) {
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
				file.write("\tpop\trbx");
				src = "rax\n";			
			}
			else if(node.children.get(0).val.type == TokenType.NUM) {
				src = node.children.get(0).val.val + "\n";
			}
			else {
				src = "[rsp+" + (node.children.get(0).val.val * memoryWidth) + "]\n";
			}
		}
		
		switch(node.val.type) {
		case PLUS:
			file.write("\tadd\trbx, " + src);
			break;
		case MIN:
			file.write("\tsub\trbx, " + src);
			break;
		case MUL:
			file.write("\tadd\trbx, " + src);
			break;
		case DIV:
			file.write("\tadd\trbx, " + src);
			break;
		case MOD:
			file.write("\tadd\trbx, " + src);
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
