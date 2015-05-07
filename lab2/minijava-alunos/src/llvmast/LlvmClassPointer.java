package llvmast;

import java.util.List;
import llvm.MethodNode;

public class LlvmClassPointer extends LlvmType {
	public String className;
        public LlvmType methodType;
        List<LlvmValue> fList;

	public LlvmClassPointer(String className, LlvmType methodType, List<LlvmValue> fList) {
		this.className = className;
                this.methodType = methodType;
                this.fList = fList;
	}

	public String toString() {
		String ret = methodType + " (%class." + className + " * ";
                for(int i=1; i<fList.size(); i++) {
                    ret = ret + ", " + fList.get(i).type.toString();
                }
                
                ret = ret + ")" +  " *";
                return ret;
	}
}