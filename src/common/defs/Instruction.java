package common.defs;

public class Instruction {
	public OpCode code;
	public Location dest;
	public int dest_offset;
	public long dest_val;
	public Location src;
	public int src_offset;
	public long src_val;
	
	public Instruction(OpCode code) {
		this.code = code;
	}
}
