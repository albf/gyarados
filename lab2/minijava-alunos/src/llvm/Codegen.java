/*****************************************************
Esta classe Codegen é a responsável por emitir LLVM-IR. 
Ela possui o mesmo método 'visit' sobrecarregado de
acordo com o tipo do parâmetro. Se o parâmentro for
do tipo 'While', o 'visit' emitirá código LLVM-IR que 
representa este comportamento. 
Alguns métodos 'visit' já estão prontos e, por isso,
a compilação do código abaixo já é possível.

class a{
    public static void main(String[] args){
    	System.out.println(1+2);
    }
}

O pacote 'llvmast' possui estruturas simples 
que auxiliam a geração de código em LLVM-IR. Quase todas 
as classes estão prontas; apenas as seguintes precisam ser 
implementadas: 

// llvmasm/LlvmBranch.java
// llvmasm/LlvmIcmp.java
// llvmasm/LlvmMinus.java
// llvmasm/LlvmTimes.java


Todas as assinaturas de métodos e construtores 
necessárias já estão lá. 


Observem todos os métodos e classes já implementados
e o manual do LLVM-IR (http://llvm.org/docs/LangRef.html) 
como guia no desenvolvimento deste projeto. 

 ****************************************************/
package llvm;

import semant.Env;
import syntaxtree.*;
import llvmast.*;

import java.util.*;

public class Codegen extends VisitorAdapter {
	private List<LlvmInstruction> assembler;
	private Codegen codeGenerator;

	private SymTab symTab;
	private ClassNode classEnv; // Aponta para a classe atualmente em uso em
								// symTab
	private MethodNode methodEnv; // Aponta para a metodo atualmente em uso em
									// symTab

	public Codegen() {
		assembler = new LinkedList<LlvmInstruction>();
		symTab = new SymTab();
	}

	// Método de entrada do Codegen
	public String translate(Program p, Env env) {
		codeGenerator = new Codegen();

		// Preenchendo a Tabela de Símbolos
		// Quem quiser usar 'env', apenas comente essa linha
		codeGenerator.symTab.FillTabSymbol(p);

		// Formato da String para o System.out.printlnijava "%d\n"
		codeGenerator.assembler.add(new LlvmConstantDeclaration(
				"@.formatting.string",
				"private constant [4 x i8] c\"%d\\0A\\00\""));

		// NOTA: sempre que X.accept(Y), então Y.visit(X);
		// NOTA: Logo, o comando abaixo irá chamar codeGenerator.visit(Program),
		// linha 75
		p.accept(codeGenerator);

		// Link do printf
		List<LlvmType> pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		pts.add(LlvmPrimitiveType.DOTDOTDOT);
		codeGenerator.assembler.add(new LlvmExternalDeclaration("@printf",
				LlvmPrimitiveType.I32, pts));
		List<LlvmType> mallocpts = new LinkedList<LlvmType>();
		mallocpts.add(LlvmPrimitiveType.I32);
		codeGenerator.assembler.add(new LlvmExternalDeclaration("@malloc",
				new LlvmPointer(LlvmPrimitiveType.I8), mallocpts));

		String r = new String();
		for (LlvmInstruction instr : codeGenerator.assembler) {
			r += instr + "\n";
			//System.out.println(instr+"\n");
		}
		return r;
	}

	public LlvmValue visit(Program n) {

		System.err.println("\nNode: " + n.getClass().getName());

		n.mainClass.accept(this);

		for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
			c.head.accept(this);

		return null;
	}

	public LlvmValue visit(MainClass n) {

		System.err.println("Node: " + n.getClass().getName());

		// definicao do main
		assembler.add(new LlvmDefine("@main", LlvmPrimitiveType.I32,
				new LinkedList<LlvmValue>()));
		assembler.add(new LlvmLabel(new LlvmLabelValue("entry")));
		LlvmRegister R1 = new LlvmRegister(new LlvmPointer(
				LlvmPrimitiveType.I32));
		assembler.add(new LlvmAlloca(R1, LlvmPrimitiveType.I32,
				new LinkedList<LlvmValue>()));
		assembler.add(new LlvmStore(new LlvmIntegerLiteral(0), R1));

		// Statement é uma classe abstrata
		// Portanto, o accept chamado é da classe que implementa Statement, por
		// exemplo, a classe "Print".
		n.stm.accept(this);

		// Final do Main
		LlvmRegister R2 = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmLoad(R2, R1));
		assembler.add(new LlvmRet(R2));
		assembler.add(new LlvmCloseDefinition());
		return null;
	}

	/* Block node */
	public LlvmValue visit(Block n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Call accept in each Statement of the Block */
		for (util.List<Statement> stmts = n.body; stmts != null; stmts = stmts.tail) {
			stmts.head.accept(this); // Accept the head
		}
		return null;
	}
	
	public LlvmValue visit(Call n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Get the Object Reference */
		LlvmValue objReff = n.object.accept(this);
		
		/* Get the list of arguments */
		List<LlvmValue> args = new ArrayList<>();
		List<LlvmType> args_t = new ArrayList<>();
		
		/* Get the name of the method */
		String classType = objReff.type.toString();
		String clTypeName = classType.substring(7, classType.indexOf(" "));
		
		// DEBUG
		//System.err.println(clTypeName+"\n");
		
		/* Get the class and the method */
		ClassNode classNode = symTab.classes.get(clTypeName);
		MethodNode methodNode = classNode.mList.get(n.method.s);
		
		/* Checks the type of the first argument */
		LlvmRegister this_ptr = new LlvmRegister(methodNode.fList.get(0).type);
		
		/* Checks with the obj type */
		if (this_ptr.type.toString().equals(objReff.type.toString()))
			args.add(objReff);
		else {
			assembler.add(new LlvmLoad(this_ptr, objReff));
			args.add(this_ptr);
		}
		
		/* The remaining arguments */
		for (util.List<Exp> vec = n.actuals; vec != null; vec = vec.tail) {
			/* Issues the instructions to deal with formals */
			LlvmValue tmp = vec.head.accept(this);
			
			/* Deals with double pointers */
			if (tmp.type.toString().contains("* *")) {
				LlvmValue a_lhs = new LlvmRegister(((LlvmPointer) tmp.type).content);
				assembler.add(new LlvmLoad(a_lhs, tmp));
				args.add(a_lhs);
			} else
				args.add(tmp);
		}
		
		/* Add the types in the type Array */
		for (LlvmValue val : methodNode.fList)
			args_t.add(val.type);
		
		/* Declares the name of the function */
		String fnName = "@__"+methodNode.mName+"__"+clTypeName;
		
		/* Issues the call instruction to the method */
		LlvmRegister lhs = new LlvmRegister(methodNode.rType);
		assembler.add(
				new LlvmCall(
					lhs,
					methodNode.rType,
					fnName,
					args
				)
		);

		return null;
	}
	
	/* ClassDecSimple node */
	public LlvmValue visit(ClassDeclSimple n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Get the actual class */
		classEnv = symTab.classes.get(n.name.toString());
		
		/* Deal with the instructions for the methods */
		for (util.List<MethodDecl> met = n.methodList; met != null; met = met.tail)
			met.head.accept(this);

		return null;
	}
	
	/* Formal node */
	public LlvmValue visit(Formal n) {

		System.err.println("Node: " + n.getClass().getName());

		return new LlvmNamedValue("%"+n.name, (n.type.accept(this)).type);
	}
	
	/* Identifier node */
	public LlvmValue visit(Identifier n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Look for the variable in the list of locals */
		LlvmValue var = methodEnv.vMap.get(n.s);
		if (var == null) {
			var = classEnv.attrMap.get(n.s);
		}

		return new LlvmNamedValue(var.toString(), var.type);
	}
	
	/* IdentifierExp node */
	public LlvmValue visit(IdentifierExp n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Accept in the child */
		LlvmValue addr = n.name.accept(this);
		
		/* Gets the type of the identifier */
		LlvmRegister lhs = new LlvmRegister(addr.type);
		
		/* Issues the Instruction */
		assembler.add(new LlvmLoad(lhs, new LlvmNamedValue(addr+"_tmp", new LlvmPointer(addr.type)))); 

		return lhs;
	}
	
	/* IdentifierType node */
	public LlvmValue visit(IdentifierType n) {

		System.err.println("Node: " + n.getClass().getName());

		return new LlvmRegister(n.name, new LlvmClassType(n.name));
	}
	
	/* If node */
	public LlvmValue visit(If n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Child nodes from If node */
		LlvmValue cond = n.condition.accept(this);
		Statement thenClause = n.thenClause;
		Statement elseClause = n.elseClause;

		/* Used to demark uniquily the if statement */
		int line = n.line, row = n.row;

		/* Create the labels string */
		String ifthen = "IfThen_" + line + "-" + row;
		String ifelse = "IfElse_" + line + "-" + row;
		String ifend = "IfEnd_" + line + "-" + row;

		/* Check the body type (if-then or if-then-else) */
		if (elseClause != null) {
			assembler.add(new LlvmBranch(cond,
					new LlvmLabelValue("%" + ifthen), new LlvmLabelValue("%"
							+ ifelse)));
		} else {
			assembler.add(new LlvmBranch(cond,
					new LlvmLabelValue("%" + ifthen), new LlvmLabelValue("%"
							+ ifend)));
		}

		/* Insert label to thenClause */
		assembler.add(new LlvmLabel(new LlvmLabelValue(ifthen)));
		/* Insert IRs for the body of then clause */
		thenClause.accept(this);
		/* Insert IRs for jump to the end of if */
		assembler.add(new LlvmBranch(new LlvmLabelValue("%" + ifend)));

		/* Case there is an else clause */
		if (elseClause != null) {
			/* Insert label to elseClause */
			assembler.add(new LlvmLabel(new LlvmLabelValue(ifelse)));
			/* Insert IRs */
			elseClause.accept(this);
			/* Insert IRs to jump to the end of if */
			assembler.add(new LlvmBranch(new LlvmLabelValue("%" + ifend)));
		}

		/* Insert label ifend */
		assembler.add(new LlvmLabel(new LlvmLabelValue(ifend)));

		return null;

	}
	
	/* IntegerLiteral node */
	public LlvmValue visit(IntegerLiteral n) {

		System.err.println("Node: " + n.getClass().getName());

		return new LlvmIntegerLiteral(n.value);
	}
	
	/* IntegerType node */
	public LlvmValue visit(IntegerType n) {

		System.err.println("Node: " + n.getClass().getName());

		return new LlvmNamedValue("", LlvmPrimitiveType.I32);
	}

	/* LessThan node */
	public LlvmValue visit(LessThan n) {

		System.err.println("Node: " + n.getClass().getName());

		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		assembler.add(new LlvmIcmp(lhs, 1, v1.type, v1, v2));
		return lhs;

	}
	
	/* MethodDecl node */
	public LlvmValue visit(MethodDecl n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Sets the actual method */
		methodEnv = classEnv.mList.get(n.name.s);
		
		/* Method Name */
		String mName = "@__"+methodEnv.mName+"__"+classEnv.className;
		
		/* Control of names */
		List<String> varList = new ArrayList<>();
		
		/* Get the formal list and the local list */
		List<LlvmValue> formals = methodEnv.fList;
		List<LlvmValue> locals = methodEnv.vList;
		
		/* Get the return type */
		LlvmType rType = methodEnv.rType;
		
		/* Define the method - Print the instructions */
		assembler.add(new LlvmDefine(mName, rType, formals));
		assembler.add(new LlvmLabel(new LlvmLabelValue("entry")));
		
		/* Declare the method formals */
		for (LlvmValue arg : formals) {
			/* Allocate the memory to the formal */
			LlvmNamedValue lhs = new LlvmNamedValue(arg.toString()+"_tmp", arg.type);
			assembler.add(new LlvmAlloca(lhs, arg.type, new ArrayList<LlvmValue>()));
			/* Store the value */
			LlvmNamedValue plhs = new LlvmNamedValue(arg.toString()+"_tmp", new LlvmPointer(arg.type));
			assembler.add(new LlvmStore(new LlvmNamedValue(arg.toString(), arg.type), plhs));
			
			/* Updates the list of vars */
			varList.add(arg.toString());
		}
		
		/* Declare the locals variables */
		for (LlvmValue var : locals) {
			/* Skip double declaration */
			if (varList.contains(var.toString()))	continue;
			
			/* Allocate the memory to the formal */
			LlvmNamedValue lhs = new LlvmNamedValue(var.toString()+"_tmp", var.type);
			assembler.add(new LlvmAlloca(lhs, var.type, new ArrayList<LlvmValue>()));
			
			/* Updates the list of vars */
		}
		
		/* Issues the body instructions */
		for (util.List<Statement> stmts = n.body; stmts != null; stmts = stmts.tail)
			stmts.head.accept(this);
		
		/* Return */		
		LlvmValue rValue = n.returnExp.accept(this);
		assembler.add(new LlvmRet(rValue));
		
		/* Close the method */
		assembler.add(new LlvmCloseDefinition());		

		return n.returnType.accept(this);
	}
	
	/* Minus node */
	public LlvmValue visit(Minus n) {

		System.err.println("Node: " + n.getClass().getName());

		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmMinus(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;
	}
	
	/* NewObject node */
	public LlvmValue visit(NewObject n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Issues the className identifier */
		LlvmRegister lhs = new LlvmRegister(new LlvmPointer(symTab.classes.get(n.className.s).type));
		assembler.add(new LlvmMalloc(lhs, lhs.type, "%class."+n.className.s));

		/* Return */
		return lhs;
	}

	/* Plus node */
	public LlvmValue visit(Plus n) {

		System.err.println("Node: " + n.getClass().getName());

		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmPlus(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;
	}	

	/* Print node */
	public LlvmValue visit(Print n) {

		System.err.println("Node: " + n.getClass().getName());

		LlvmValue v = n.exp.accept(this);

		// getelementptr:
		LlvmRegister lhs = new LlvmRegister(new LlvmPointer(
				LlvmPrimitiveType.I8));
		LlvmRegister src = new LlvmNamedValue("@.formatting.string",
				new LlvmPointer(new LlvmArray(4, LlvmPrimitiveType.I8)));
		List<LlvmValue> offsets = new LinkedList<LlvmValue>();
		offsets.add(new LlvmIntegerLiteral(0));
		offsets.add(new LlvmIntegerLiteral(0));
		List<LlvmType> pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		List<LlvmValue> args = new LinkedList<LlvmValue>();
		args.add(lhs);
		args.add(v);
		assembler.add(new LlvmGetElementPointer(lhs, src, offsets));

		pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		pts.add(LlvmPrimitiveType.DOTDOTDOT);

		// printf:
		assembler.add(new LlvmCall(new LlvmRegister(LlvmPrimitiveType.I32),
				LlvmPrimitiveType.I32, pts, "@printf", args));
		return null;
	}
	
	/* Times node */
	public LlvmValue visit(Times n) {

		System.err.println("Node: " + n.getClass().getName());

		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmTimes(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;

	}
	
	/* VarDecl node */
	public LlvmValue visit(VarDecl n) {

		System.err.println("Node: " + n.getClass().getName());

		return new LlvmNamedValue("%"+n.name.toString(), (n.type.accept(this)).type);
	}

	/* While node */
	public LlvmValue visit(While n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Get the condition and the body of the while */
		LlvmValue cond;
		Statement wBody = n.body;

		/* To make the Labels uniques */
		int line = n.line, row = n.row;

		/* Prepare the string to the labels */
		String wCond = "WhileCond_" + line + "-" + row;
		String wBegin = "WhileBegin_" + line + "-" + row;
		String wEnd = "WhileEnd_" + line + "-" + row;

		/* Insert branch instruction to the test of the loop */
		assembler.add(new LlvmBranch(new LlvmLabelValue("%" + wCond)));

		/* Insert the Label to the wCond */
		assembler.add(new LlvmLabel(new LlvmLabelValue(wCond)));

		/* Insert the branch instruction and the begin label */
		cond = n.condition.accept(this);
		assembler.add(new LlvmBranch(cond, new LlvmLabelValue("%" + wBegin),
				new LlvmLabelValue("%" + wEnd)));
		assembler.add(new LlvmLabel(new LlvmLabelValue(wBegin)));

		/* Insert the body of the loop */
		wBody.accept(this);

		/* Insert the branch to the check condition */
		assembler.add(new LlvmBranch(new LlvmLabelValue("%" + wCond)));

		/* Insert the end label */
		assembler.add(new LlvmLabel(new LlvmLabelValue(wEnd)));

		return null;
	}
	
	/* ====================================================================== */	
	/* ====================================================================== */
	// Todos os visit's que devem ser implementados
	/* ====================================================================== */
	/* ====================================================================== */

	public LlvmValue visit(ClassDeclExtends n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(IntArrayType n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(Assign n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(ArrayAssign n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(And n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(Equal n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(ArrayLookup n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(ArrayLength n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(True n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(False n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(This n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(NewArray n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(Not n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}
}
