package common.defs;

import common.util.OSVersion;

public class Registers {
	public final static String[] stackRegisters = setupStackRegisters(); 
	public final static String[] preserveRegisters = setupPreserveRegisters();
	
	
	private static String[] setupStackRegisters() {
		String[] winCallRegisters = {"rcx", "rdx", "r8", "r9"};
		String[] unixCallRegisters = {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};
		return OSVersion.isWindows() ? winCallRegisters : unixCallRegisters;
	}
	
	private static String[] setupPreserveRegisters() {
		String[] winPreserveRegisters = { "rbx", "rdi", "rsi", "r12", "r13", "r14", "r15" };
		String[] unixPreserveRegisters = { "rbx", "r12", "r13", "r14", "r15" };
		return OSVersion.isWindows() ? winPreserveRegisters : unixPreserveRegisters;
	}
}
