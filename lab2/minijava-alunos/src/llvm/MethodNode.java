/**********************************************************************************/
/*
 * === Method Node ====
 */
/**********************************************************************************/
package llvm;

import java.util.List;
import llvmast.*;

public class MethodNode {
	public String mName;
	public List<LlvmValue> vList;
	public List<LlvmValue> fList;
	public LlvmType rType;

	public MethodNode(String name, List<LlvmValue> vars,
			List<LlvmValue> formals, LlvmType type) {
		this.mName = name;
		this.vList = vars;
		this.fList = formals;
		this.rType = type;
	}
}