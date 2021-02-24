package interpreter;

import java.util.ArrayList;

import common.defs.*;

public class VirtualMachine {
	public static String preamble = "call main\next\nnop\nnop\nnop\nnop\nnop\nnop\nnop\nnop\n";
	public static String[] builtInFnName = { "print" };
	
	private long[] registers = new long[Registers.list.length];
	private ArrayList<Instruction> codeSegment;
	private long[] dataSegment;
	
	private final int RIP = 0;
	private final int RAX = 1;
	private final int RCX = 3;
	private final int RDX = 4;
	private final int RSP = 7;
	
	public VirtualMachine(String programText, int memorySize) throws Exception {
		dataSegment = new long[memorySize];
		codeSegment = Tokenizer.parseProgram(programText);
		registers[RIP] = 0;
		registers[RSP] = memorySize - 1;
	}
	
	public void run() throws Exception {
		long temp = 0, arg1 = 0, arg2 = 0;
		
		while(true) {
			Instruction i = codeSegment.get((int) registers[RIP]);
			
			switch(i.code) {
			case ADD:
			case MOV:
			case SUB:
			case XOR:
				if(i.src == Location.IMED) arg2 = i.src_val;
				else if(i.src == Location.REG) arg2 = registers[(int) i.src_val];
				else arg2 = dataSegment[(int) registers[(int) i.src_val] + i.src_offset];
			case CALL:
			case CALLB:
			case DIV:
			case IMUL:
			case PUSH:
				if(i.dest == Location.IMED) arg1 = i.dest_val;
				else if(i.dest == Location.REG) arg1 = registers[(int) i.dest_val];
				else arg1 = dataSegment[(int) registers[(int) i.dest_val] + i.dest_offset];
				break;
			default:
				break;
			}
			
			
			switch(i.code) {
			case ADD:
				temp = arg1 + arg2;
				if(i.dest == Location.REG) registers[(int) i.dest_val] = temp;
				else dataSegment[(int) registers[(int) (i.dest_val)] + i.dest_offset] = temp;
				break;
			case CALL:
				registers[RSP] -= 8;
				dataSegment[(int) registers[RSP]] = registers[RIP] + 1;
				registers[RIP] = arg1;
				continue;
			case CALLB:
				print((int) registers[RCX]);
				break;
			case DIV:
				temp = registers[RAX] / arg1;
				long remain = registers[RAX] % arg1;
				registers[RAX] = temp;
				registers[RDX] = remain;
				break;
			case EXT:
				return;
			case IMUL:
				temp = registers[RAX] * arg1;
				registers[RAX] = temp;
				break;
			case MOV:
				if(i.dest == Location.REG) registers[(int) i.dest_val] = arg2;
				else dataSegment[(int) registers[(int) (i.dest_val)] + i.dest_offset] = arg2;
				break;
			case NOP:
				break;
			case POP:
				if(i.dest == Location.REG) registers[(int) i.dest_val] = dataSegment[(int) registers[RSP]];
				else dataSegment[(int) registers[(int) (i.dest_val)] + i.dest_offset] = dataSegment[(int) registers[RSP]];
				registers[RSP] += 8;
				break;
			case PUSH:
				registers[RSP] -= 8;
				dataSegment[(int) registers[RSP]] = arg1;
				break;
			case RET:
				temp = dataSegment[(int) registers[RSP]];
				registers[RSP] += 8;
				registers[RIP] = temp;
				continue;
			case SUB:
				temp = arg1 - arg2;
				if(i.dest == Location.REG) registers[(int) i.dest_val] = temp;
				else dataSegment[(int) registers[(int) (i.dest_val)] + i.dest_offset] = temp;
				break;
			case XOR:
				temp = arg1 ^ arg2;
				if(i.dest == Location.REG) registers[(int) i.dest_val] = temp;
				else dataSegment[(int) registers[(int) (i.dest_val)] + i.dest_offset] = temp;
				break;
			default:
				throw new Exception("Virtual MAchine code error: " + codeSegment.get((int) registers[0]).code);
			}
			registers[RIP]++;
		}
	}
	
	private void print(int x) {
		System.out.println(x);
	}
}
