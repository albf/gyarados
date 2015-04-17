/**********************************************************************************/
/*
 * === Tabela de Simbolos ====
 */
/**********************************************************************************/

package llvm;

import semant.Env;
import syntaxtree.*;
import llvmast.*;

import java.util.*;

public class SymTab extends VisitorAdapter {
	public Map<String, ClassNode> classes;
	private ClassNode classEnv; // aponta para a classe em uso

	public LlvmValue FillTabSymbol(Program n) {
		n.accept(this);
		return null;
	}

	public LlvmValue visit(Program n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		n.mainClass.accept(this);

		for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
			c.head.accept(this);

		return null;
	}

	public LlvmValue visit(MainClass n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		classes.put(n.className.s, new ClassNode(n.className.s, null, null));
		return null;
	}

	public LlvmValue visit(ClassDeclSimple n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		List<LlvmType> typeList = null;
		// Constroi TypeList com os tipos das variáveis da Classe (vai formar a
		// Struct da classe)

		List<LlvmValue> varList = null;
		// Constroi VarList com as Variáveis da Classe

		classes.put(n.name.s, new ClassNode(n.name.s, new LlvmStructure(
				typeList), varList));
		// Percorre n.methodList visitando cada método
		return null;
	}

	public LlvmValue visit(ClassDeclExtends n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		return null;
	}

	public LlvmValue visit(VarDecl n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		return null;
	}

	public LlvmValue visit(Formal n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		return null;
	}

	public LlvmValue visit(MethodDecl n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		return null;
	}

	public LlvmValue visit(IdentifierType n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		return null;
	}

	public LlvmValue visit(IntArrayType n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		return null;
	}

	public LlvmValue visit(BooleanType n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		return null;
	}

	public LlvmValue visit(IntegerType n) {

		System.err.println("SysTab Visit: "+ n.getClass().getName());

		return null;
	}
}