package llvmast;

public class LlvmIcmp extends LlvmInstruction {
	public LlvmRegister lhs;
	public LlvmType type;
	public LlvmValue op1, op2;
	public int conditionCode;

	/* Initialize the Object */
	public LlvmIcmp(LlvmRegister lhs, int conditionCode, LlvmType type,
			LlvmValue op1, LlvmValue op2) {

		this.lhs = lhs;
		this.type = type;
		this.op1 = op1;
		this.op2 = op2;
		this.conditionCode = conditionCode;

	}

	/* Write the ICMP Instruction */
	public String toString() {
		String c = null;

		switch (conditionCode) {
		case 0:
			c = "eq";
			break;
		case 1:
			c = "slt";
			break;
		}
		return "  " + lhs + " = icmp " + c + " " + type + " " + op1 + ", "
				+ op2;
	}
}