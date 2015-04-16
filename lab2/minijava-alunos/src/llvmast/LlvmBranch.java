package llvmast;

public class LlvmBranch extends LlvmInstruction {
	public LlvmValue cond;
	public LlvmLabelValue brTrue, brFalse;

	/* Unconditional branch Object */
	public LlvmBranch(LlvmLabelValue label) {
		this.brTrue = label;
		this.brFalse = null;
		this.cond = null;
	}

	/* Conditional branch Object */
	public LlvmBranch(LlvmValue cond, LlvmLabelValue brTrue,
			LlvmLabelValue brFalse) {
		this.cond = cond;
		this.brTrue = brTrue;
		this.brFalse = brFalse;
	}

	/* Write the Instructions to do a Branch */
	public String toString() {

		String tmp = null;
		/* Checks the unconditional and conditional jump */
		if (cond == null) {
			tmp = "  " + "br " + LlvmPrimitiveType.LABEL + " " + brTrue;
		} else {
			tmp = "  " + "br i1 " + cond + ", " + LlvmPrimitiveType.LABEL + " "
					+ brTrue + ", " + LlvmPrimitiveType.LABEL + " " + brFalse;
		}

		/* returns one of the two strings */
		return tmp;

	}
}