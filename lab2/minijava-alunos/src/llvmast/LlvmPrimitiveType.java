package llvmast;

public class LlvmPrimitiveType extends LlvmType {
	public static final LlvmType I1 = new LlvmPrimitiveType();
	public static final LlvmType I8 = new LlvmPrimitiveType();
	public static final LlvmType I32 = new LlvmPrimitiveType();
	public static final LlvmType VOID = new LlvmPrimitiveType();
	public static final LlvmType LABEL = new LlvmPrimitiveType();
	public static final LlvmType DOTDOTDOT = new LlvmPrimitiveType();
        public static final LlvmType VTable = new LlvmPrimitiveType();
        //public static final LlvmType i8pp = new LlvmPrimitiveType();
        
	public String toString() {
		if (this == I1)
			return "i1";
		if (this == I8)
			return "i8";
		if (this == I32)
			return "i32";
		if (this == VOID)
			return "void";
		if (this == LABEL)
			return "label";
		if (this == DOTDOTDOT)
			return "...";
                if(this == VTable)
                        return "[1 x i8 *] *";
                //if(this == i8pp)
                //        return "i8 * *";
		return null;
	}
}