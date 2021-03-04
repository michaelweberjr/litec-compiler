package common.defs;

public enum OpCode {
	NOP,
	MOV, PUSH, POP,
	EXT, CALL, CALLB, RET,
	NEG, INC, DEC,
	ADD, SUB, IMUL, DIV,
	NOT, XOR, OR, AND, SHL, SHR,
	CMP, JMP, JE, JNE, JL, JLE, JG, JGE,
}
