package llvmast;

/* Based on LlvmPlus */
public class LlvmTimes extends LlvmInstruction {
	public LlvmRegister lhs;
	public LlvmType type;
	public LlvmValue op1, op2;

	/* Initialize the Object */
	public LlvmTimes(LlvmRegister lhs, LlvmType type, LlvmValue op1,
			LlvmValue op2) {
		this.lhs = lhs;
		this.type = type;
		this.op1 = op1;
		this.op2 = op2;
	}

	/* Write the Times Instruction */
	public String toString() {
		return "  " + lhs + " = mul " + type + " " + op1 + ", " + op2;
	}
}
