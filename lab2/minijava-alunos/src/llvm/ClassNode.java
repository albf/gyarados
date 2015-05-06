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
	public String className;
	public LlvmStructure classType;
	public LlvmClassType type;
	public List<LlvmValue> varList;
	public LinkedHashMap<String, MethodNode> mList;
	public Map<String, LlvmValue> attrMap;
        public boolean isExtended;
        public String superName;

	/* Constructor */
	ClassNode(String nameClass, LlvmStructure classType,
			List<LlvmValue> varList, Map<String, LlvmValue> attrMap) {
		this.className = nameClass;
		this.classType = classType;
		this.varList = varList;
		this.mList = new LinkedHashMap<>();
		this.type = new LlvmClassType(nameClass);
		this.attrMap = attrMap;
                this.isExtended = false;
                this.superName = null;
	}
        
        	/* Constructor */
	ClassNode(String nameClass, LlvmStructure classType,
			List<LlvmValue> varList, Map<String, LlvmValue> attrMap, boolean isExtended, String superName) {
		this.className = nameClass;
		this.classType = classType;
		this.varList = varList;
		this.mList = new LinkedHashMap<>();
		this.type = new LlvmClassType(nameClass);
		this.attrMap = attrMap;
                this.isExtended = isExtended;
                this.superName = superName;
	}
}