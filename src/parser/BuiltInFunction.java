package parser;

import java.util.ArrayList;

import parser.defs.FunctionTree;
import parser.defs.Symbol;
import parser.defs.Token;
import parser.defs.TokenType;
import parser.util.OSVersion;

public class BuiltInFunction {
	
	public static FunctionTree printFnTree() {
		FunctionTree print = new FunctionTree();
		print.name = new Symbol("print");
		(print.argsList = new ArrayList<Symbol>()).add(new Symbol("value"));
		print.returnType = new Token(TokenType.RET);
		print.isBuiltIn = true;
		print.builtInCode = printFnCode();
		return print;
	}
	
	public static String printFnCode() {
		return OSVersion.isWindows() 
				// windows version
				? "extern printf\n\n"
				+ "SECTION .DATA\n"
				+"\tfmt:\tdb \"%d\", 10, 0\n\n"
				+ "SECTION .TEXT\n\n"
				+ "print:\n"
				+ "\tmov	rcx,fmt\n"
				+ "\tmov	rdx,[rsp+8]\n"
				+ "\tcall   printf\n"
				+ "\tpop	rbp\n"
				+ "\tret\n\n"
				// everyone else
				: "extern printf\n\n"
				+ "SECTION .DATA\n"
				+"\tfmt:\tdb \"%d\", 10, 0\n\n"
				+ "SECTION .TEXT\n\n"
				+ "print:\n"
				+ "\tmov	rdi,fmt\n"
				+ "\tmov	rsi,[rsp+8]\n"
				+ "\tmov	rax,0\n"
				+ "\tcall    printf\n"
				+ "\tpop	rbp\n"
				+ "\tret\n\n";
	}
}
