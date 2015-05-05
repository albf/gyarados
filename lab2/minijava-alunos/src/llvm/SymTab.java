/**********************************************************************************/
/*
 * === Tabela de Simbolos ====
 */
/**********************************************************************************/

package llvm;

import syntaxtree.*;
import llvmast.*;

import java.util.*;

public class SymTab extends VisitorAdapter {
	public Map<String, ClassNode> classes;
	private ClassNode classEnv; // aponta para a classe em uso

	public SymTab() {
		classes = new HashMap<>();
	}

	public LlvmValue FillTabSymbol(Program n) {
		n.accept(this);
		return null;
	}

	public LlvmValue visit(Program n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		n.mainClass.accept(this);

		for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
			c.head.accept(this);

		return null;
	}

	public LlvmValue visit(MainClass n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		classes.put(n.className.s, new ClassNode(n.className.s, null, null,
				null));
		return null;
	}

	public LlvmValue visit(ClassDeclSimple n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		/* Should think about that ARRAYLIST */
		// Constroi TypeList com os tipos das variáveis da Classe (vai formar a
		// Struct da classe)
		List<LlvmType> typeList = new ArrayList<>();

		// Constroi VarList com as Variáveis da Classe
		List<LlvmValue> varList = new ArrayList<>();

		/* Creates the attribute map */
		Map<String, LlvmValue> attr = new HashMap<>();

		/* Populate the arrays */
		System.err.println(" Class: " + n.name);
		for (util.List<VarDecl> vec = n.varList; vec != null; vec = vec.tail) {
			System.err.println(" | attribute: " + vec.head.name);

			LlvmValue variable = vec.head.accept(this);

			/* Checks if is an object */
			if (variable.type.toString().contains("%class."))
				variable.type = new LlvmPointer(variable.type);

			typeList.add(variable.type);
			varList.add(variable);
			attr.put(vec.head.name.s, variable);
		}

		classEnv = new ClassNode(n.name.toString(),
				new LlvmStructure(typeList), varList, attr);

		classes.put(n.name.toString(), classEnv);

		// Percorre n.methodList visitando cada método
		System.err.println(" Class: " + n.name);
		for (util.List<MethodDecl> vec = n.methodList; vec != null; vec = vec.tail) {
			System.err.println("    | method: " + vec.head.name);

			vec.head.accept(this);
		}

		return null;
	}

	public LlvmValue visit(ClassDeclExtends n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		/* Should think about that ARRAYLIST */
		// Constroi TypeList com os tipos das variáveis da Classe (vai formar a
		// Struct da classe)
		List<LlvmType> typeList = new ArrayList<>();

		// Constroi VarList com as Variáveis da Classe
		List<LlvmValue> varList = new ArrayList<>();

		/* Creates the attribute map */
		Map<String, LlvmValue> attr = new HashMap<>();
                
                /* Add the Father */
                //LlvmValue father = new LlvmNamedValue(n.superClass.toString(), n.superClass.);
                System.err.println("SymTab Visit: " + n.getClass().getName() + " - Adding Father : " + n.superClass.toString());
                LlvmType FatherType = new LlvmClassType(n.superClass.toString());
                LlvmValue Father = new LlvmNamedValue(n.superClass.toString(), new LlvmPointer(FatherType));
                varList.add(Father);
                typeList.add(FatherType);
                
		/* Populate the arrays of class variables */
		System.err.println(" Class: " + n.name);
		for (util.List<VarDecl> vec = n.varList; vec != null; vec = vec.tail) {
			System.err.println(" | attribute: " + vec.head.name);

			LlvmValue variable = vec.head.accept(this);

			/* Checks if is an object */
			if (variable.type.toString().contains("%class."))
				variable.type = new LlvmPointer(variable.type);

			typeList.add(variable.type);
			varList.add(variable);
			attr.put(vec.head.name.s, variable);
		}

		classEnv = new ClassNode(n.name.toString(),
				new LlvmStructure(typeList), varList, attr, true);

		classes.put(n.name.toString(), classEnv);

		// Percorre n.methodList visitando cada método
		System.err.println(" Class: " + n.name);
		for (util.List<MethodDecl> vec = n.methodList; vec != null; vec = vec.tail) {
			System.err.println("    | method: " + vec.head.name);
			vec.head.accept(this);
		}

		return null;
	}

	public LlvmValue visit(VarDecl n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());
		// System.err.println(n.toString());

		/* Printing the code of the variable declaration */
		LlvmValue type = n.type.accept(this);
		LlvmValue name = n.name.accept(this);

		/* Getting the type and name of the variable to return */
		LlvmNamedValue variable = new LlvmNamedValue("%" + n.name.toString(),
				type.type);

		return variable;
	}

	public LlvmValue visit(Formal n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		/* Printing the code of the variable declaration */
		LlvmValue type = n.type.accept(this);

		/* Getting the type and name of the variable to return */
		LlvmNamedValue formal = new LlvmNamedValue("%" + n.name.toString(),
				type.type);

		return formal;
	}

	public LlvmValue visit(MethodDecl n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		/* Define the Method signature */
		String methodName = "@__" + n.name.toString() + "__"
				+ classEnv.className;

		/* Variable and Arguments Lists */
		List<LlvmValue> vList = new ArrayList<>();
		List<LlvmValue> fList = new ArrayList<>();

		/* Variable Map - avoids formals and locals with the same name */
		Map<String, LlvmValue> vMap = new HashMap<>();

		/* Building the Formal List */
		//LlvmNamedValue tmp = new LlvmNamedValue("%this", new LlvmClassType(
		//		classEnv.className));

		/* First Argument is always the Object */
		LlvmNamedValue object = new LlvmNamedValue("%this", new LlvmPointer(new LlvmClassType(classEnv.className)));
                fList.add(object);
		vMap.put(object.name, object);

		/* Add the list of formals */
		for (util.List<Formal> vec = n.formals; vec != null; vec = vec.tail) {
			/*
			 * Print the instructions to the arguments Add this values to the
			 * list of formals
			 */
			LlvmValue v = vec.head.accept(this);
			fList.add(v);
			vMap.put(v.toString().substring(1), v);
                        System.err.println("SymTab Visit: " + n.getClass().getName() + " - Adding Formal : " + v.toString());
		}

		/* Add the list of variables */
		for (util.List<VarDecl> vec = n.locals; vec != null; vec = vec.tail) {
			/*
			 * Print the instructions to the locals Add this values to the list
			 * of locals
			 */
			LlvmValue v = vec.head.accept(this);
			vList.add(v);
			vMap.put(vec.head.name.s, v);
                        System.err.println("SymTab Visit: " + n.getClass().getName() + " - Adding VarDecl : " + v.toString());
			// System.err.println(vMap.toString());
		}
                System.err.println("SymTab Visit: " + n.getClass().getName() + " - vList : " + vList.toString());

		/* Add the method to the Class Node */
                LlvmValue returnFix = n.returnType.accept(this);
                System.err.println("SymTab Visit: " + n.getClass().getName() + " Adding Method. ReturnFix: " + returnFix.toString());
                System.err.println("SymTab Visit: " + n.getClass().getName() + " Type: " + returnFix.type);
               
                if(!returnFix.type.toString().contains("%class.")) {
                    classEnv.mList.put(n.name.toString(), new MethodNode(n.name.toString(),
			vList, fList, returnFix.type, vMap));
                }
                else {
                    System.err.println("SymTab Visit: " + n.getClass().getName() + " - Class type, using pointer.");
                    classEnv.mList.put(n.name.toString(), new MethodNode(n.name.toString(),
			vList, fList, new LlvmPointer(returnFix.type), vMap));
                }

                System.err.println("SymTab Visit: " + n.getClass().getName() + " Just added Method.");
		return null;
	}

	public LlvmValue visit(IdentifierType n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		//return new LlvmRegister(n.name, new LlvmClassType(n.name));
                System.err.println("SymTab Visit: " + n.getClass().getName() + " - n.name: " + n.name);
                return new LlvmNamedValue (n.name, new LlvmClassType(n.name));
        }

	public LlvmValue visit(IntArrayType n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		return new LlvmRegister(new LlvmPointer(new LlvmArray(0,
				LlvmPrimitiveType.I32)));
	}

	public LlvmValue visit(BooleanType n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		return new LlvmNamedValue("", LlvmPrimitiveType.I1);
	}

	public LlvmValue visit(IntegerType n) {

		System.err.println("SymTab Visit: " + n.getClass().getName());

		return new LlvmNamedValue("", LlvmPrimitiveType.I32);
	}
}