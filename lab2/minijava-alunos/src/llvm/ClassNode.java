/**********************************************************************************/
/*
 * === Class Node ====
 */
/**********************************************************************************/

package llvm;

import semant.Env;
import syntaxtree.*;
import llvmast.*;

import java.util.*;

public class ClassNode extends LlvmType {
	public String nameClass;
	public LlvmStructure classType;
	public List<LlvmValue> varList;
	public Map<String, MethodNode> mList;

	/* Constructor */
	ClassNode(String nameClass, LlvmStructure classType, List<LlvmValue> varList) {
		this.nameClass = nameClass;
		this.classType = classType;
		this.varList = varList;
		this.mList = new HashMap<>();
	}
}