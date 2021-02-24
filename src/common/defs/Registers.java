package common.defs;

import common.util.OSVersion;

public class Registers {
	public final static String[] stackRegisters = setupStackRegisters(); 
	public final static String[] preserveRegisters = setupPreserveRegisters();
	public final static String[] list = {"rip", "rax", "rbx", "rcx", "rdx", "rdi", "rsi", "rsp", "rbp", "r8", "r9", "r10", "r11", "r12", "r13", "r14", "r15"};
	
	
	private static String[] setupStackRegisters() {
		String[] winCallRegisters = {"rcx", "rdx", "r8", "r9"};
		String[] unixCallRegisters = {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};
		return OSVersion.isWindows() ? winCallRegisters : unixCallRegisters;
	}
	
	private static String[] setupPreserveRegisters() {
		String[] winPreserveRegisters = { "rcx", "rdx", "r8", "r9", "rdi", "rsi", "rbx", "r12", "r13", "r14", "r15" };
		String[] unixPreserveRegisters = { "rdi", "rsi", "rdx", "rcx", "r8", "r9", "rbx", "r12", "r13", "r14", "r15" };
		return OSVersion.isWindows() ? winPreserveRegisters : unixPreserveRegisters;
	}
}
