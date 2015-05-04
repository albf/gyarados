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
		// System.out.println(codeGenerator.assembler.toString());
		for (LlvmInstruction instr : codeGenerator.assembler) {
                        try{
                            System.out.println(instr+"\n");
                            r += instr + "\n";
                        }
                        catch (java.lang.NullPointerException e) {
                            System.out.println("NULL POINTER");
                        }
		}
		return r;
	}

	public LlvmValue visit(Program n) {

		System.err.println("\nNode: " + n.getClass().getName());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting MainClass");
		n.mainClass.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from MainClass");

		for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail) {
                        System.err.println("\nNode: " + n.getClass().getName() + " - Accepting ClassDecl: " + c.toString());
			c.head.accept(this);
                        System.err.println("\nNode: " + n.getClass().getName() + " - Returning from ClassDecl: " + c.toString());
                }

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
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting Statement");
		n.stm.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from Statement");

		// Final do Main
		LlvmRegister R2 = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmLoad(R2, R1));
		assembler.add(new LlvmRet(R2));
		assembler.add(new LlvmCloseDefinition());
		return null;
	}
	
	/* Assign node */
	public LlvmValue visit(Assign n) {
		
		System.err.println("Node: " + n.getClass().getName());
		
		/* Visits the var and the expression in the right of the assignment */
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting lhs.");
		LlvmValue lhs_accept = n.var.accept(this);
                LlvmValue lhs = new LlvmNamedValue(lhs_accept.toString() + "_tmp", new LlvmPointer(lhs_accept.type));
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning From lhs. Value: " + lhs.toString() + " - Type: " + lhs.type.toString());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting rhs.");
		LlvmValue rhs = n.exp.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returnin From rhs. Value: " + rhs.toString() + " - Type: " + rhs.type.toString());
		LlvmValue cast;

		/* Test if the two types are the same */
		if (lhs_accept.type.toString().equals(rhs.type.toString())) {
                        System.err.println("Node: " + n.getClass().getName() + " - Same type, no cast.");
			assembler.add(new LlvmStore(rhs, lhs));
		} else {
			/* Cast the value if they aren't */
                        System.err.println("Node: " + n.getClass().getName() + " - Different type, cast needed.");
			cast = new LlvmRegister(new LlvmPointer(rhs.type));
			assembler.add(new LlvmBitcast(cast, lhs, cast.type));
			assembler.add(new LlvmStore(rhs, cast));
		}

		return null;
	}

	/* Block node */
	public LlvmValue visit(Block n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Call accept in each Statement of the Block */
		for (util.List<Statement> stmts = n.body; stmts != null; stmts = stmts.tail) {
                        System.err.println("\nNode: " + n.getClass().getName() + " - Accepting Statement");
			stmts.head.accept(this); // Accept the head
                        System.err.println("\nNode: " + n.getClass().getName() + " - Returning from Statement");
		}
		return null;
	}

	public LlvmValue visit(Call n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Get the Object Reference */
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting objReff");
		LlvmValue objReff = n.object.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from objReff");

		/* Get the list of arguments */
		List<LlvmValue> args = new ArrayList<>();
		List<LlvmType> args_t = new ArrayList<>();

		/* Get the name of the method */
		String classType = objReff.type.toString();
		String clTypeName = classType.substring(7, classType.indexOf(" "));

		// DEBUG
		// System.err.println(clTypeName+"\n");

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
                        System.err.println("\nNode: " + n.getClass().getName() + " - Accepting Argument");
			LlvmValue tmp = vec.head.accept(this);
                        System.err.println("\nNode: " + n.getClass().getName() + " - Returning from Argument");

			/* Deals with double pointers */
			if (tmp.type.toString().contains("* *")) {
				LlvmValue a_lhs = new LlvmRegister(
						((LlvmPointer) tmp.type).content);
				assembler.add(new LlvmLoad(a_lhs, tmp));
				args.add(a_lhs);
			} else
				args.add(tmp);
		}

		/* Add the types in the type Array */
		for (LlvmValue val : methodNode.fList)
			args_t.add(val.type);

		/* Declares the name of the function */
		String fnName = "@__" + methodNode.mName + "__" + clTypeName;

		/* Issues the call instruction to the method */
		LlvmRegister lhs = new LlvmRegister(methodNode.rType);
		assembler.add(new LlvmCall(lhs, methodNode.rType, fnName, args));

		return lhs;
	}

	/* ClassDecSimple node */
	public LlvmValue visit(ClassDeclSimple n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Get the actual class */
		classEnv = symTab.classes.get(n.name.toString());
                LlvmConstantDeclaration ClassDef = new LlvmConstantDeclaration("%class." + classEnv.className, "type " + classEnv.classType);
                assembler.add(0, ClassDef);

		/* Deal with the instructions for the methods */
		for (util.List<MethodDecl> met = n.methodList; met != null; met = met.tail) {
                        System.err.println("\nNode: " + n.getClass().getName() + " - Accepting Method");
			met.head.accept(this);
                        System.err.println("\nNode: " + n.getClass().getName() + " - Returning from Method");
                }

		return null;
	}

	/* Formal node */
	public LlvmValue visit(Formal n) {

		System.err.println("Node: " + n.getClass().getName());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting FormalValue");
                LlvmNamedValue FormalValue = new LlvmNamedValue("%" + n.name, (n.type.accept(this)).type);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from FormalValue");
		return FormalValue;
	}

	/* Identifier node */
	public LlvmValue visit(Identifier n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Look for the variable in the list of locals */
		LlvmValue var = methodEnv.vMap.get(n.s);
		if (var == null) {
                        System.err.println("Node: " + n.getClass().getName() + "- Using Class Variable");
			var = classEnv.attrMap.get(n.s);
		}
                else {
                    System.err.println("Node: " + n.getClass().getName() + "- Using Method Variable");
                }

		return new LlvmNamedValue(var.toString(), var.type);
	}

	/* IdentifierExp node */
	public LlvmValue visit(IdentifierExp n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Accept in the child */
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting addr");
		LlvmValue addr = n.name.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from addr");

		/* Gets the type of the identifier */
		LlvmRegister lhs = new LlvmRegister(addr.type);

		/* Issues the Instruction */
		assembler.add(new LlvmLoad(lhs, new LlvmNamedValue(addr + "_tmp",
				new LlvmPointer(addr.type))));

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
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting Cond");
		LlvmValue cond = n.condition.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from Cond");
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
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting thenClause");
		thenClause.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from thenClause");
		/* Insert IRs for jump to the end of if */
		assembler.add(new LlvmBranch(new LlvmLabelValue("%" + ifend)));

		/* Case there is an else clause */
		if (elseClause != null) {
			/* Insert label to elseClause */
			assembler.add(new LlvmLabel(new LlvmLabelValue(ifelse)));
			/* Insert IRs */
                        System.err.println("\nNode: " + n.getClass().getName() + " - Accepting elseClause");
			elseClause.accept(this);
                        System.err.println("\nNode: " + n.getClass().getName() + " - Returning from elseClause");
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
                
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting v1");
		LlvmValue v1 = n.lhs.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from v1: " + v1.toString());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting v2");
		LlvmValue v2 = n.rhs.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from v2" + v2.toString());
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
		String mName = "@__" + methodEnv.mName + "__" + classEnv.className;

		/* Control of names */
		List<String> varList = new ArrayList<>();

		/* Get the formal list and the local list */
		List<LlvmValue> formals = methodEnv.fList;
		List<LlvmValue> locals = methodEnv.vList;

		/* Get the return type */
		LlvmType rType = methodEnv.rType;
                
                System.err.println("Node: " + n.getClass().getName() + " - rType: " + rType.toString());
		/* Define the method - Print the instructions */
		assembler.add(new LlvmDefine(mName, rType, formals));
		assembler.add(new LlvmLabel(new LlvmLabelValue("entry")));

		/* Declare the method formals */
		for (LlvmValue arg : formals) {
			/* Allocate the memory to the formal */
			LlvmNamedValue lhs = new LlvmNamedValue(arg.toString() + "_tmp",
					arg.type);
			assembler.add(new LlvmAlloca(lhs, arg.type,
					new ArrayList<LlvmValue>()));
			/* Store the value */
			LlvmNamedValue plhs = new LlvmNamedValue(arg.toString() + "_tmp",
					new LlvmPointer(arg.type));
			assembler.add(new LlvmStore(new LlvmNamedValue(arg.toString(),
					arg.type), plhs));

			/* Updates the list of vars */
			varList.add(arg.toString());
		}

		/* Declare the locals variables */
		for (LlvmValue var : locals) {
			/* Skip double declaration */
			if (varList.contains(var.toString()))
				continue;

			/* Allocate the memory to the formal */
			LlvmNamedValue lhs = new LlvmNamedValue(var.toString() + "_tmp",
					var.type);
			assembler.add(new LlvmAlloca(lhs, var.type,
					new ArrayList<LlvmValue>()));

			/* Updates the list of vars */
		}

                /* Allocate space for class variables, check if it's a method name first */
                int counter = 0;
                
                for(LlvmValue classvar: classEnv.varList) {
                    boolean ShouldInclude = true;
                    int locals_size = locals.size();
                    for(int i=0 ; i<locals_size; i++) {
                        if(locals.get(i).toString().equals(classvar.toString())) {
                            ShouldInclude = false;
                            break;
                        }
                    }
                    int formals_size = formals.size();
                    if(ShouldInclude) {
                        for(int i=0 ; i<formals_size; i++) {
                            if(formals.get(i).toString().equals(classvar.toString())) {
                                ShouldInclude = false;
                                break;
                            }
                        }
                    }
                    //System.err.println("DEBUG locals : " + locals.toString());
                    //System.err.println("ShouldInclude : " + Boolean.toString(ShouldInclude));
                    if(ShouldInclude) {
                        LlvmValue assign = new LlvmNamedValue(classvar.toString() + "_tmp", new LlvmPointer(classvar.type));
                        LlvmValue value = new LlvmNamedValue("%this", new LlvmPointer(new LlvmClassType(classEnv.className)));
                        LinkedList<LlvmValue> offset = new LinkedList<LlvmValue>();
                        LlvmValue offset1 = new LlvmNamedValue("0", LlvmPrimitiveType.I32);
                        LlvmValue offset2 = new LlvmNamedValue(Integer.toString(counter), LlvmPrimitiveType.I32);
                        counter = counter+1;
                        offset.add(offset1);
                        offset.add(offset2);
                        assembler.add(new LlvmGetElementPointer(assign, value, offset));
                    }
                }
                
                // Debug
		System.err.println("METHOD");
		for (util.List<Statement> stmts = n.body; stmts != null; stmts = stmts.tail)
			System.err.println(stmts.toString());

		/* Issues the body instructions */
		for (util.List<Statement> stmts = n.body; stmts != null; stmts = stmts.tail) {
                        System.err.println("\nNode: " + n.getClass().getName() + " - Accepting statement");
			stmts.head.accept(this);
                        System.err.println("\nNode: " + n.getClass().getName() + " - Returning from statement");
		}

		/* Return */
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting rValue");
		LlvmValue rValue = n.returnExp.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from rValue");
		assembler.add(new LlvmRet(rValue));

		/* Close the method */
		assembler.add(new LlvmCloseDefinition());

                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting and returning n.returnType");
		return n.returnType.accept(this);
	}

	/* Minus node */
	public LlvmValue visit(Minus n) {

		System.err.println("Node: " + n.getClass().getName());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting v1");
		LlvmValue v1 = n.lhs.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from v1: " + v1.toString());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting v2");
		LlvmValue v2 = n.rhs.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from v2: " + v2.toString());
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmMinus(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;
	}

	/* NewObject node */
	public LlvmValue visit(NewObject n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Issues the className identifier */
		LlvmRegister lhs = new LlvmRegister(new LlvmPointer(
				symTab.classes.get(n.className.s).type));
		assembler.add(new LlvmMalloc(lhs, lhs.type, "%class." + n.className.s));

		/* Return */
		return lhs;
	}

	/* Plus node */
	public LlvmValue visit(Plus n) {

		System.err.println("Node: " + n.getClass().getName());

                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting v1");
		LlvmValue v1 = n.lhs.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from v1: " + v1.toString());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting v2");
		LlvmValue v2 = n.rhs.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from v2: " + v2.toString());
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmPlus(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;
	}

	/* Print node */
	public LlvmValue visit(Print n) {
		System.err.println("Node: " + n.getClass().getName());

                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting v");
		LlvmValue v = n.exp.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from v: " + v.toString());
                System.err.println("\nNode: " + n.getClass().getName() + " - v.type: " + v.type);

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
                LlvmRegister PrintReg = new LlvmRegister(LlvmPrimitiveType.I32);
                    LlvmCall PrintCall = new LlvmCall(PrintReg,
				LlvmPrimitiveType.I32, pts, "@printf", args);
                System.err.println("Node: " + n.getClass().getName() + " - PrintReg : " + PrintReg.toString());
                System.err.println("Node: " + n.getClass().getName() + " - LlvmCall : " + PrintCall.toString());
                System.err.println("Node: " + n.getClass().getName() + " - Args : " + args.toString());
                System.err.println("Node: " + n.getClass().getName() + " - Pts : " + pts.toString());
		assembler.add(PrintCall);
                //assembler.add(new LlvmCall(new LlvmRegister(LlvmPrimitiveType.I32),
		//		LlvmPrimitiveType.I32, pts, "@printf", args));
		return null;
	}
	
	/* This node */
	public LlvmValue visit(This n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Returns a register that points to the class */
		LlvmRegister lhs = new LlvmRegister(new LlvmPointer(new LlvmClassType(classEnv.className)));
		assembler.add(new LlvmLoad(lhs, new LlvmNamedValue("%this_tmp", new LlvmPointer(lhs.type))));

		return lhs;
	}

	/* Times node */
	public LlvmValue visit(Times n) {

		System.err.println("Node: " + n.getClass().getName());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting v1");
		LlvmValue v1 = n.lhs.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from v1: " + v1.toString());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting v2");
		LlvmValue v2 = n.rhs.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from v2: " + v2.toString());
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmTimes(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;

	}

	/* VarDecl node */
	public LlvmValue visit(VarDecl n) {

		System.err.println("Node: " + n.getClass().getName());
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting n.type and returning new LlvNamedValue");
		return new LlvmNamedValue("%" + n.name.toString(),
				(n.type.accept(this)).type);
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
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting cond");
		cond = n.condition.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from cond");
		assembler.add(new LlvmBranch(cond, new LlvmLabelValue("%" + wBegin),
				new LlvmLabelValue("%" + wEnd)));
		assembler.add(new LlvmLabel(new LlvmLabelValue(wBegin)));

		/* Insert the body of the loop */
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting body");
		wBody.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from cond");

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
                LlvmValue base = n.array.accept(this);
                
                LlvmRegister elem_ptr = new LlvmRegister(new LlvmPointer(((LlvmArray)((LlvmPointer)base.type).content).content));
                List<LlvmValue> offset = new LinkedList<LlvmValue>();
                offset.add(new LlvmIntegerLiteral(0));
                
                LlvmValue index = n.index.accept(this);
                if(index instanceof LlvmIntegerLiteral) {
                    offset.add(new LlvmIntegerLiteral(((LlvmIntegerLiteral )index).value+1));
                }
                else {
                    LlvmRegister fix = new LlvmRegister(LlvmPrimitiveType.I32);
                    assembler.add(new LlvmPlus(fix, fix.type, index, new LlvmIntegerLiteral(1)));
                    offset.add(fix);
                }
                
                assembler.add(new LlvmGetElementPointer (elem_ptr, base, offset));
                
                LlvmRegister assign = new LlvmRegister(((LlvmArray)((LlvmPointer)(base.type)).content).content);
                assembler.add(new LlvmLoad(assign, elem_ptr));
                return assign;       
	}

	public LlvmValue visit(ArrayLength n) {

		System.err.println("Node: " + n.getClass().getName());

                LlvmValue len = n.array.accept(this);
                LlvmValue len_value = new LlvmNamedValue(len.toString(), len.type);
                LlvmValue assign = new LlvmRegister(LlvmPrimitiveType.I32);
                LlvmValue cast = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
                assembler.add(new LlvmBitcast(cast, len_value, cast.type));
                assembler.add(new LlvmLoad(assign,cast));
                System.err.println("Node: " + n.getClass().getName() + " - assign.toString: " + assign.toString());
                System.err.println("Node: " + n.getClass().getName() + " - assign.type: " + assign.type.toString());
		return assign;
	}

	public LlvmValue visit(True n) {

		System.err.println("Node: " + n.getClass().getName());
                LlvmValue TrueValue = new LlvmBool(1);
		return TrueValue;
	}

	public LlvmValue visit(False n) {

		System.err.println("Node: " + n.getClass().getName());
                LlvmValue FalseValue = new LlvmBool(0);
		return FalseValue;
	}

	public LlvmValue visit(NewArray n) {
                System.err.println("Node: " + n.getClass().getName());
            
                // Allocation and store of the new array
                LlvmValue array = new LlvmRegister(new LlvmArray(0, LlvmPrimitiveType.I32));
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting n.size");
                LlvmValue size = n.size.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from n.size");
                assembler.add(new LlvmMalloc(array, LlvmPrimitiveType.I32, size));
                array.type = new LlvmPointer(LlvmPrimitiveType.I32);
                assembler.add(new LlvmStore(LlvmMalloc.lastArraySize, array));
	
		return array;
	}

	public LlvmValue visit(Not n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}
}
