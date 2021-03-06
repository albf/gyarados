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
        private String calledArgument;

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
		
		/* Reset the register counter */
		LlvmRegister.rewind();

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
			try {
				System.out.println(instr+"\n");
				r += instr + "\n";
			} catch (java.lang.NullPointerException e) {
				System.out.println("NULL POINTER");
			}
		}
		return r;
	}

	public LlvmValue visit(Program n) {

		System.err.println("\nNode: " + n.getClass().getName());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting MainClass");
		n.mainClass.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from MainClass");

		for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail) {
			System.err.println("\nNode: " + n.getClass().getName()
					+ " - Accepting ClassDecl: " + c.toString());
			c.head.accept(this);
			System.err.println("\nNode: " + n.getClass().getName()
					+ " - Returning from ClassDecl: " + c.toString());
		}

		return null;
	}

	public LlvmValue visit(MainClass n) {

		System.err.println("Node: " + n.getClass().getName());
                
                // Adding ref
                /* Get the actual class */
		classEnv = symTab.classes.get(n.className.toString());
                String attr = "type { }";
                LlvmConstantDeclaration ClassDef = new LlvmConstantDeclaration("%class." + classEnv.className, attr);
		assembler.add(0, ClassDef);

		// definicao do main
		assembler.add(new LlvmDefine("@main", LlvmPrimitiveType.I32,
				new LinkedList<LlvmValue>()));
		assembler.add(new LlvmLabel(new LlvmLabelValue("entry")));
		LlvmRegister R1 = new LlvmRegister(new LlvmPointer(
				LlvmPrimitiveType.I32));
		assembler.add(new LlvmAlloca(R1, LlvmPrimitiveType.I32, new LinkedList<LlvmValue>()));
		assembler.add(new LlvmStore(new LlvmIntegerLiteral(0), R1));

		// Statement é uma classe abstrata
		// Portanto, o accept chamado é da classe que implementa Statement, por
		// exemplo, a classe "Print".
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting Statement");
		n.stm.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from Statement");

		// Final do Main
		LlvmRegister R2 = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmLoad(R2, R1));
		assembler.add(new LlvmRet(R2));
		assembler.add(new LlvmCloseDefinition());
		return null;
	}
	
            /* And node */
	public LlvmValue visit(And n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Accept the two values */
		LlvmValue lChild = n.lhs.accept(this);
                
		/* Create the register */
		LlvmRegister lhs = new LlvmRegister(lChild.type);
                LlvmRegister ret = new LlvmRegister(LlvmPrimitiveType.I1);
                assembler.add(new LlvmAlloca(ret, LlvmPrimitiveType.I1, new ArrayList<LlvmValue>()));
                
                
                System.err.println("Node: " + n.getClass().getName());

		/* Child nodes from If node */
		System.err.println("\nNode: " + n.getClass().getName() + " - Accepting Cond");
		LlvmValue cond = lChild;
		System.err.println("\nNode: " + n.getClass().getName() + " - Returning from Cond");

		/* Used to demark uniquily the if statement */
		int line = n.line, row = n.row;

		/* Create the labels string */
		String ifthen = "IfThen_And_" + line + "-" + row;
		String ifelse = "IfElse_And_" + line + "-" + row;
		String ifend = "IfEnd_And_" + line + "-" + row;

		/* Check the body type (if-then or if-then-else) */
	//	if (elseClause != null) {
                    
                assembler.add(new LlvmBranch(cond, new LlvmLabelValue("%" + ifthen), new LlvmLabelValue("%" + ifelse)));
	//	} else {
                //    assembler.add(new LlvmBranch(cond, new LlvmLabelValue("%" + ifthen), new LlvmLabelValue("%" + ifend)));
	//	}
                    
                /* Insert label to thenClause */
		assembler.add(new LlvmLabel(new LlvmLabelValue(ifthen)));
		/* Insert IRs for the body of then clause */
                
		LlvmValue rChild = n.rhs.accept(this);
                //assembler.add(new LlvmAnd(ret, lChild.type, lChild, rChild));
                ret.type = new LlvmPointer(LlvmPrimitiveType.I1);
                assembler.add(new LlvmStore(rChild, ret));   

                /* Issues the instruction */
		//  maassembler.add(new LlvmAnd(lhs, lChild.type, lChild, rChild));
		
                
		/* Insert IRs for jump to the end of if */
		assembler.add(new LlvmBranch(new LlvmLabelValue("%" + ifend)));

                // if else
		assembler.add(new LlvmLabel(new LlvmLabelValue(ifelse)));
		/* Insert IRs */
		System.err.println("\nNode: " + n.getClass().getName() + " - Accepting elseClause");
		//elseClause.accept(this);
		//assembler.add(new LlvmMinus(ret, LlvmPrimitiveType.I1, new LlvmBool(0), new LlvmBool(0)));
                assembler.add(new LlvmStore(new LlvmBool(0), ret)); 
                        
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from elseClause");
                /* Insert IRs to jump to the end of if */
		
                
                assembler.add(new LlvmBranch(new LlvmLabelValue("%" + ifend)));
		

		/* Insert label ifend */
		assembler.add(new LlvmLabel(new LlvmLabelValue(ifend)));
                LlvmRegister ret_loaded = new LlvmRegister(LlvmPrimitiveType.I1);
                
                assembler.add(new LlvmLoad(ret_loaded, ret));


		return ret_loaded;
                
	}

	/* Assign node */
	public LlvmValue visit(Assign n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Visits the var and the expression in the right of the assignment */
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting lhs.");
		LlvmValue lhs_accept = n.var.accept(this);
		LlvmValue lhs = new LlvmNamedValue(lhs_accept.toString() + "_tmp",
				new LlvmPointer(lhs_accept.type));
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning From lhs. Value: " + lhs.toString()
				+ " - Type: " + lhs.type.toString());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting rhs.");
		LlvmValue rhs = n.exp.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returnin From rhs. Value: " + rhs.toString()
				+ " - Type: " + rhs.type.toString());
		LlvmValue cast;

		/* Test if the two types are the same */
		if (lhs_accept.type.toString().equals(rhs.type.toString())) {
			System.err.println("Node: " + n.getClass().getName()
					+ " - Same type, no cast.");
			assembler.add(new LlvmStore(rhs, lhs));
		} else {
			/* Cast the value if they aren't */
			System.err.println("Node: " + n.getClass().getName()
					+ " - Different type, cast needed.");
			cast = new LlvmRegister(new LlvmPointer(rhs.type));
			assembler.add(new LlvmBitcast(cast, lhs, cast.type));
			assembler.add(new LlvmStore(rhs, cast));
		}

		return null;
	}
	
	/* ArrayAssign node */
	public LlvmValue visit(ArrayAssign n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Get a pointer to the base value */
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting base pointer");
		LlvmValue basePtr = n.var.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - returning from base pointer");

		/* Get the base value */
		LlvmValue base = new LlvmRegister(new LlvmPointer(
				((LlvmPointer) basePtr.type).content));

		/* Pointer to the first position */
		LlvmRegister fstPtr = new LlvmRegister(new LlvmPointer(
				((LlvmArray) ((LlvmPointer) base.type).content).content));
		List<LlvmValue> offList = new ArrayList<>();
		offList.add(new LlvmIntegerLiteral(0));
		
		/* Issues the Instruction to load the array */
		assembler.add(new LlvmLoad(base, new LlvmNamedValue(basePtr.toString() + "_tmp", new LlvmPointer(base.type))));
		
		/* Accept the index */
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting index");
		LlvmValue ind = n.index.accept(this);		
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from index");
		
		if (ind instanceof LlvmIntegerLiteral) {
			offList.add(new LlvmIntegerLiteral(((LlvmIntegerLiteral)ind).value+1));
		} else {
			LlvmRegister lhsPlus = new LlvmRegister(LlvmPrimitiveType.I32);
			offList.add(lhsPlus);
			assembler.add(new LlvmPlus(lhsPlus, lhsPlus.type, ind, new LlvmIntegerLiteral(1)));
		}
		
		/* Issues the index instruction */
		assembler.add(new LlvmGetElementPointer(fstPtr, base, offList));
		System.err.println("\nNode: " + n.getClass().getName() + " - Accepting index value");
		LlvmValue val = n.value.accept(this);
		System.err.println("\nNode: " + n.getClass().getName() + " - Returning from index value");
		assembler.add(new LlvmStore(val, fstPtr));
		
		return null;
	}

	/* ArrayLength node */
	public LlvmValue visit(ArrayLength n) {

		System.err.println("Node: " + n.getClass().getName());

		LlvmValue len = n.array.accept(this);
		LlvmValue len_value = new LlvmNamedValue(len.toString(), len.type);
		LlvmValue assign = new LlvmRegister(LlvmPrimitiveType.I32);
		LlvmValue cast = new LlvmRegister(
				new LlvmPointer(LlvmPrimitiveType.I32));
		assembler.add(new LlvmBitcast(cast, len_value, cast.type));
		assembler.add(new LlvmLoad(assign, cast));
		System.err.println("Node: " + n.getClass().getName()
				+ " - assign.toString: " + assign.toString());
		System.err.println("Node: " + n.getClass().getName()
				+ " - assign.type: " + assign.type.toString());
		return assign;
	}

	/* ArrayLookup node */
	public LlvmValue visit(ArrayLookup n) {

		System.err.println("Node: " + n.getClass().getName());
		LlvmValue base = n.array.accept(this);

		LlvmRegister elem_ptr = new LlvmRegister(new LlvmPointer(
				((LlvmArray) ((LlvmPointer) base.type).content).content));
		List<LlvmValue> offset = new LinkedList<LlvmValue>();
		offset.add(new LlvmIntegerLiteral(0));

		LlvmValue index = n.index.accept(this);
		if (index instanceof LlvmIntegerLiteral) {
			offset.add(new LlvmIntegerLiteral(
					((LlvmIntegerLiteral) index).value + 1));
		} else {
			LlvmRegister fix = new LlvmRegister(LlvmPrimitiveType.I32);
			assembler.add(new LlvmPlus(fix, fix.type, index,
					new LlvmIntegerLiteral(1)));
			offset.add(fix);
		}

		assembler.add(new LlvmGetElementPointer(elem_ptr, base, offset));

		LlvmRegister assign = new LlvmRegister(
				((LlvmArray) ((LlvmPointer) (base.type)).content).content);
		assembler.add(new LlvmLoad(assign, elem_ptr));
		return assign;
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
                String clTypeName;

		/* Get the name of the method */
		String classType = objReff.type.toString();
                System.err.println("\nNode: " + n.getClass().getName() + " - classType: " + classType);
		try{
                    clTypeName = classType.substring(7, classType.indexOf(" "));
                } catch (StringIndexOutOfBoundsException e) {
                    clTypeName = classType.substring(7);
                }
		// DEBUG
		// System.err.println(clTypeName+"\n");

		/* Get the class and the method */
		ClassNode classNode = symTab.classes.get(clTypeName);
		MethodNode methodNode = classNode.mList.get(n.method.s);
                
                // UNNECESSARY BECAUSE OF JOIN
                /* Look if should get the method from the father(s)
                while (methodNode == null) {
                    ClassNode classPai = symTab.classes.get(classNode.superName);
                    LlvmValue objPai = new LlvmRegister(new LlvmPointer(classPai.type));
                    assembler.add(new LlvmBitcast(objPai, objReff, objPai.type));
                    // bitCast
                    
                    objReff = objPai;
                    classNode = classPai;
                    methodNode = classNode.mList.get(n.method.s);
                } */ 

		/* Checks the type of the first argument */
		//LlvmRegister this_ptr = new LlvmRegister(methodNode.fList.get(0).type);

		/* Checks with the obj type */
		/*if (this_ptr.type.toString().equals(objReff.type.toString()))
			args.add(objReff);
		else {
			assembler.add(new LlvmLoad(this_ptr, objReff));
			args.add(this_ptr);
		}*/
                System.err.println("\nNode: " + n.getClass().getName() + " - classEnv.className: " + classEnv.className);
                System.err.println("\nNode: " + n.getClass().getName() + " - clTypeName: " + clTypeName + " - len: " + clTypeName.length());
                System.err.println("\nNode: " + n.getClass().getName() + " - methodNode.mName: " + methodNode.mName + " - len: " + methodNode.mName.length());
                /*if(classEnv.className.equals(clTypeName)) {
                    LlvmType argType = new LlvmClassType(clTypeName);
                    LlvmValue thisArg = new LlvmNamedValue("%this", new LlvmPointer(argType));
                    args.add(thisArg);
                    System.err.println("\nNode: " + n.getClass().getName() + " - EQUAL ARGS: " + args.toString());
                } */
                //else {
                    LlvmRegister this_ptr = new LlvmRegister(methodNode.fList.get(0).type);
                    if (this_ptr.type.toString().equals(objReff.type.toString())) {
                        System.err.println("\nNode: " + n.getClass().getName() + " - DIFF_A ");
			args.add(objReff);
                    }
                    else {
                        System.err.println("\nNode: " + n.getClass().getName() + " - DIFF_B ");
                        System.err.println("\nNode: " + n.getClass().getName() + " - objReff: " + objReff.toString());
                        System.err.println("\nNode: " + n.getClass().getName() + " - objReff: " + objReff.toString());
                        System.err.println("\nNode: " + n.getClass().getName() + " - objReff: " + objReff.toString());
                        System.err.println("\nNode: " + n.getClass().getName() + " -  methodNode.fList.get(0).type: " + methodNode.fList.get(0).type.toString() );
			//assembler.add(new LlvmLoad(this_ptr, objReff));
			//args.add(this_ptr);
                        LlvmType argType = new LlvmClassType(clTypeName);
                        LlvmValue thisArg = new LlvmNamedValue(this.calledArgument, new LlvmPointer(argType));
                        args.add(thisArg);
                        
                    }
                    System.err.println("\nNode: " + n.getClass().getName() + " - DIFFERENT ARGS: " + args.toString());
                //}
                
		/* The remaining arguments */
                int counter = 1;
		for (util.List<Exp> vec = n.actuals; vec != null; vec = vec.tail) {
			/* Issues the instructions to deal with formals */
			System.err.println("\nNode: " + n.getClass().getName() + " - Accepting Argument");
			LlvmValue tmp = vec.head.accept(this);
			System.err.println("\nNode: " + n.getClass().getName() + " - Returning from Argument, TMP_TYPE: " + tmp.type.toString());
                        System.err.println("\nNode: " + n.getClass().getName() + " - methodNode.mName: " + methodNode.mName + " - len: " + methodNode.mName.length());
                        
			/* Deals with double pointers */
			if (tmp.type.toString().contains("* *")) {
				LlvmValue a_lhs = new LlvmRegister(((LlvmPointer) tmp.type).content);
				assembler.add(new LlvmLoad(a_lhs, tmp));
				args.add(a_lhs);
                                System.err.println("\nNode: " + n.getClass().getName() + " - TYPE1 :" + a_lhs.toString());
			} 
                        /*=else if (tmp.type.toString().contains("%class") && tmp.type.toString().contains("*")) {
                            LlvmType conv_type = new LlvmClassType(tmp.type.toString().substring(7, tmp.type.toString().length()-2));
                            // Com ou sem ponteiro
                            LlvmRegister conv = new LlvmRegister(conv_type);
                            //LlvmRegister conv = new LlvmRegister(new LlvmPointer(conv_type));
                            assembler.add(new LlvmLoad(conv, tmp));
                            args.add(conv); 
                        } */
                        
                        else if (tmp.type.toString().contains("%class") && (!tmp.type.toString().contains("*")) && (this.calledArgument != null)) {
                            //LlvmRegister conv = new LlvmRegister(new LlvmPointer(conv_type));
                            //this.calledArgument
                            LlvmType fix_type = new LlvmClassType(tmp.type.toString().substring(7));
                            LlvmValue fix = new LlvmNamedValue(this.calledArgument, new LlvmPointer(fix_type));
                            this.calledArgument = null;
                            
                            // Want father.
                            System.err.println("\nNode: " + n.getClass().getName() + " - fix.type.toString() :" + fix.type.toString());
                            System.err.println("\nNode: " + n.getClass().getName() + " - methodNode.fList.get(counter).type.toString() :" + methodNode.fList.get(counter).type.toString());
                            
                            if(!fix.type.toString().equals(methodNode.fList.get(counter).type.toString())) {
                                System.err.println("\nNode: " + n.getClass().getName() + " - WANT FATHER TYP2:");
                                ClassNode argNode = symTab.classes.get(fix.type.toString().substring(7, fix.type.toString().indexOf(" ")-1));
                                ClassNode fatherNode = symTab.classes.get(argNode.superName);
                                boolean more = true;
                                while(more) {
                                    if((new LlvmPointer(fatherNode.type)).toString().equals(methodNode.fList.get(counter).type.toString())) {
                                        more=false;
                                        LlvmValue PaiReg = new LlvmRegister(new LlvmPointer(fatherNode.type));
                                        assembler.add(new LlvmBitcast(PaiReg, fix, PaiReg.type));
                                        args.add(PaiReg);
                                    }
                                     else {
                                        fatherNode = symTab.classes.get(fatherNode.superName);
                                    }
                                }
                            }
                            else {
                                args.add(fix);
                                System.err.println("\nNode: " + n.getClass().getName() + " - TYPE2 :" + fix.toString());
                            }
                        }
                            
                        else  {
                            // Want father.
                            System.err.println("\nNode: " + n.getClass().getName() + " - tmp.type.toString() :" + tmp.type.toString());
                            System.err.println("\nNode: " + n.getClass().getName() + " - methodNode.fList.get(counter).type.toString() :" + methodNode.fList.get(counter).type.toString());
                            
                            if(!tmp.type.toString().equals(methodNode.fList.get(counter).type.toString())) {
                                System.err.println("\nNode: " + n.getClass().getName() + " - WANT FATHER TYPE3 :" + tmp.type.toString().substring(7, tmp.type.toString().indexOf(" ")));
                                ClassNode argNode = symTab.classes.get(tmp.type.toString().substring(7, tmp.type.toString().indexOf(" ")));
                                System.err.println("\nNode: " + n.getClass().getName() + " - WANT FATHER TYPE3 symTab.classes :" + symTab.classes.toString());
                                System.err.println("\nNode: " + n.getClass().getName() + " - WANT FATHER TYPE3 className :" + argNode.className);
                                System.err.println("\nNode: " + n.getClass().getName() + " - WANT FATHER TYPE3 superName :" + argNode.superName);
                                //ClassNode fatherNode = symTab.classes.get(argNode.superName);
                                /*ClassNode fatherNode = null; // = symTab.classes.get("B");
                                for (Map.Entry<String, ClassNode> entry : symTab.classes.entrySet()) {
                                    if(entry.getKey().equals(argNode.superName)) {
                                        fatherNode = entry.getValue();
                                        break;
                                    }
                                }*/
                                ClassNode fatherNode = symTab.classes.get(argNode.superName);
                                
                                System.err.println("\nNode: " + n.getClass().getName() + " - WANT FATHER TYPE3 BEFORE fatherNode :" + fatherNode.superName);
                                
                                
                                boolean more = true;
                                while(more) {
                                    System.err.println("\nNode: " + n.getClass().getName() + " - WANT FATHER TYPE3 fatherNode :" + fatherNode.superName);
                        
                                    if(fatherNode == null) {
                                        System.err.println("\nNode: " + n.getClass().getName() + " - NULL FATHER? TYPE3 :");
                                    }
                                    
                                    LlvmPointer xxx = new LlvmPointer(fatherNode.type);
                                    System.err.println("\nNode: " + n.getClass().getName() + " - WANT FATHER TYPE3 new LlvmPointer(fatherNode.type)).toString() :" + xxx.toString());
                                    System.err.println("\nNode: " + n.getClass().getName() + " - WANT FATHER TYPE3 methodNode.fList.get(counter).toString() :" + methodNode.fList.get(counter).toString());
                        
                                    if((new LlvmPointer(fatherNode.type)).toString().equals(methodNode.fList.get(counter).type.toString())) {
                                        more=false;
                                        LlvmValue PaiReg = new LlvmRegister(new LlvmPointer(fatherNode.type));
                                        assembler.add(new LlvmBitcast(PaiReg, tmp, PaiReg.type));
                                        args.add(PaiReg);
                                    }
                                    else {
                                        fatherNode = symTab.classes.get(fatherNode.superName);
                                    }
                                }
                            }
                            else {
                            args.add(tmp);
                            }
                            System.err.println("\nNode: " + n.getClass().getName() + " - TYPE3 :" + tmp.toString());   
                        }
                        counter ++;
		}

		/* Add the types in the type Array */
		for (LlvmValue val : methodNode.fList)
			args_t.add(val.type);

                // Get reference from vTable
                counter = 0;
                for (Map.Entry<String, MethodNode> entry : classNode.mList.entrySet()) {
                    if(entry.getValue().mName.equals(methodNode.mName)) {
                        break;
                    }
                    counter++;
                }
                
                LlvmValue vTable = new LlvmRegister(LlvmPrimitiveType.VTable);
                assembler.add(new LlvmBitcast(vTable, objReff, LlvmPrimitiveType.VTable));
                
                LlvmValue g_Function = new LlvmRegister(new LlvmPointer(new LlvmPointer(LlvmPrimitiveType.I8)));
                LlvmValue l_Function = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I8));
                LlvmValue function = new LlvmRegister(new LlvmClassPointer(clTypeName, methodNode.rType, methodNode.fList ));
                
                
                List<LlvmValue> offsets = new LinkedList<LlvmValue>();
		offsets.add(new LlvmIntegerLiteral(0));
                offsets.add(new LlvmIntegerLiteral(counter));
		assembler.add(new LlvmGetElementPointer(g_Function, vTable, offsets));
                assembler.add(new LlvmLoad(l_Function, g_Function));
                assembler.add(new LlvmBitcast(function, l_Function, function.type));
                
		/* Declares the name of the function */
		//String fnName = "@__" + methodNode.mName + "__" + clTypeName;
                String fnName = function.toString();
                
		/* Issues the call instruction to the method */
		LlvmRegister lhs = new LlvmRegister(methodNode.rType);
		assembler.add(new LlvmCall(lhs, methodNode.rType, fnName, args));

		return lhs;
	}

	/* ClassDecSimple node */
	public LlvmValue visit(ClassDeclSimple n) {

		System.err.println("Node: " + n.getClass().getName() + " - Current Class: " + n.name.toString());

		/* Get the actual class */
		classEnv = symTab.classes.get(n.name.toString());


                System.err.println("Node: " + n.getClass().getName() + " - classEnv.mList.size(): " + classEnv.mList.size());
                String vTableName = "[" + classEnv.mList.size() + " x i8 *]";
                String attr;
                
                // Adds vTable variable.
                if(classEnv.classType.typeList.isEmpty()) {
                    attr = "type " + "{" + vTableName + "}";
                }
                else {
                    attr = "type " + "{" + vTableName + ", " + classEnv.classType.toString().substring(1);
                }
                
                System.err.println("Node: " + n.getClass().getName() + " - attr: " + attr);
                
                LlvmConstantDeclaration ClassDef = new LlvmConstantDeclaration("%class." + classEnv.className, attr);
                
		assembler.add(0, ClassDef);

		/* Deal with the instructions for the methods */
		for (util.List<MethodDecl> met = n.methodList; met != null; met = met.tail) {
			System.err.println("\nNode: " + n.getClass().getName() + " - Accepting Method");
			met.head.accept(this);
			System.err.println("\nNode: " + n.getClass().getName() + " - Returning from Method");
		}

		return null;
	}
	
	/* Equals node */
	public LlvmValue visit(Equal n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Accept the two values */
		LlvmValue lChild = n.lhs.accept(this);
		LlvmValue rChild = n.rhs.accept(this);
		
		/* Create the register */
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		
		/* Issues the instruction */
		assembler.add(new LlvmIcmp(lhs, 0, lChild.type, lChild, rChild));

		return lhs;
	}

	/* False node */
	public LlvmValue visit(False n) {

		System.err.println("Node: " + n.getClass().getName());
		LlvmValue FalseValue = new LlvmBool(0);
		return FalseValue;
	}

	/* Formal node */
	public LlvmValue visit(Formal n) {

		System.err.println("Node: " + n.getClass().getName());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting FormalValue");
		LlvmNamedValue FormalValue = new LlvmNamedValue("%" + n.name,
				(n.type.accept(this)).type);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from FormalValue");
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

                        // Look if should get the variable from the father(s)
                        String superName = classEnv.superName;
                        while (var == null) {
                            System.err.println("Node: " + n.getClass().getName() + "- Using Parente Class Variable");
                            ClassNode classPai = symTab.classes.get(superName);
                            //LlvmValue objPai = new LlvmRegister(new LlvmPointer(classPai.type));
                            //assembler.add(new LlvmBitcast(objPai, , objPai.type));
                            // bitCast
                            
                            var = classPai.attrMap.get(n.s);
                            if(classPai.isExtended) {
                                superName = classPai.superName;
                            }
                        }
                        
                        
		} else {
			System.err.println("Node: " + n.getClass().getName() + "- Using Method Variable");
		}

		return new LlvmNamedValue(var.toString(), var.type);
	}

	/* IdentifierExp node */
	public LlvmValue visit(IdentifierExp n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Accept in the child */
		System.err.println("\nNode: " + n.getClass().getName() + " - Accepting addr, n.name: " + n.name.toString());
		LlvmValue addr = n.name.accept(this);
		System.err.println("\nNode: " + n.getClass().getName() + " - Returning from addr: " + addr.toString());

		/* Gets the type of the identifier */
                System.err.println("\nNode: " + n.getClass().getName() + " - addr.type: " + addr.type);
		LlvmRegister lhs = new LlvmRegister(addr.type);
                this.calledArgument = addr.toString() + "_tmp";

		/* Issues the Instruction */
		assembler.add(new LlvmLoad(lhs, new LlvmNamedValue(addr + "_tmp", new LlvmPointer(addr.type))));

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
			assembler.add(new LlvmBranch(cond, new LlvmLabelValue("%" + ifthen), new LlvmLabelValue("%" + ifelse)));
		} else {
			assembler.add(new LlvmBranch(cond, new LlvmLabelValue("%" + ifthen), new LlvmLabelValue("%" + ifend)));
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

	/* IntArrayType node */
	public LlvmValue visit(IntArrayType n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Returns a new pointer to an array */
		return new LlvmRegister(new LlvmPointer(new LlvmArray(0,
				LlvmPrimitiveType.I32)));
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

		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting v1");
		LlvmValue v1 = n.lhs.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from v1: " + v1.toString());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting v2");
		LlvmValue v2 = n.rhs.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from v2" + v2.toString());
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
			LlvmNamedValue lhs = new LlvmNamedValue(arg.toString() + "_tmp", arg.type);
			assembler.add(new LlvmAlloca(lhs, arg.type, new ArrayList<LlvmValue>()));
			/* Store the value */
			LlvmNamedValue plhs = new LlvmNamedValue(arg.toString() + "_tmp", new LlvmPointer(arg.type));
			assembler.add(new LlvmStore(new LlvmNamedValue(arg.toString(), arg.type), plhs));

			/* Updates the list of vars */
			varList.add(arg.toString());
		}

		/* Declare the locals variables */
		for (LlvmValue var : locals) {
			/* Skip double declaration */
			if (varList.contains(var.toString()))
				continue;

			/* Allocate the memory to the formal */
			LlvmNamedValue lhs = new LlvmNamedValue(var.toString() + "_tmp", var.type);
			assembler.add(new LlvmAlloca(lhs, var.type, new ArrayList<LlvmValue>()));

			/* Updates the list of vars */
		}

                /* Allocate space for class variables, check if it's a method name first */
                int counter = 0;
                LinkedHashMap<String, String> MyClassVars = new LinkedHashMap<String, String>();
                
                for(LlvmValue classvar: classEnv.varList) {
                    MyClassVars.put(classvar.toString(), classEnv.toString());
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
                        
                        System.err.println("Node: " + n.getClass().getName() + " Adding ClassVar: " + classvar.toString() );
                        LlvmValue assign = null;
                        if(symTab.classes.get(classvar.toString()) == null) {
                            assign = new LlvmNamedValue(classvar.toString() + "_tmp", new LlvmPointer(classvar.type));
                        }
                        else {
                            assign = new LlvmNamedValue("%" + classvar.toString() + "_tmp", new LlvmPointer(classvar.type));
                        }
                        LlvmValue value = new LlvmNamedValue("%this", new LlvmPointer(new LlvmClassType(classEnv.className)));
                        LinkedList<LlvmValue> offset = new LinkedList<LlvmValue>();
                        LlvmValue offset1 = new LlvmNamedValue("0", LlvmPrimitiveType.I32);
                        counter = counter+1;
                        LlvmValue offset2 = new LlvmNamedValue(Integer.toString(counter), LlvmPrimitiveType.I32);
                        
                        offset.add(offset1);
                        offset.add(offset2);
                        assembler.add(new LlvmGetElementPointer(assign, value, offset));
                    }
                }
                
                // Add parent class variables
                boolean is_parent = classEnv.isExtended;
                String superName = classEnv.superName;
                while(is_parent) {
                    ClassNode classPai = symTab.classes.get(superName);
                    //int counter2 = 0;
                    counter = 1;
                    for(LlvmValue classvar: classPai.varList) {
                        if(MyClassVars.get(classvar.toString()) == null) {
                            MyClassVars.put(classvar.toString(), classPai.toString());
                            
                            LlvmValue assign = null;
                            if(symTab.classes.get(classvar.toString()) == null) {
                                assign = new LlvmNamedValue(classvar.toString() + "_tmp", new LlvmPointer(classvar.type));
                            }
                            else {
                                assign = new LlvmNamedValue("%" + classvar.toString() + "_tmp", new LlvmPointer(classvar.type));
                            }
                            LlvmValue regT = new LlvmRegister(new LlvmPointer(classPai.type));
                            LlvmValue value = new LlvmNamedValue("%this", new LlvmPointer(new LlvmClassType(classEnv.className)));
                            assembler.add(new LlvmBitcast(regT, value, regT.type));
                            
                            LinkedList<LlvmValue> offset = new LinkedList<LlvmValue>();
                            LlvmValue offset1 = new LlvmNamedValue("0", LlvmPrimitiveType.I32);
                            //counter = counter+1;
                            LlvmValue offset2 = new LlvmNamedValue(Integer.toString(counter), LlvmPrimitiveType.I32);

                            offset.add(offset1);
                            offset.add(offset2);
                            assembler.add(new LlvmGetElementPointer(assign, regT, offset));                            
                        }
                        counter++;
                    }
                        //LlvmValue objPai = new LlvmRegister(new LlvmPointer(classPai.type));
                        //assembler.add(new LlvmBitcast(objPai, , objPai.type));
                        // bitCast
                            
                    if(classPai.isExtended) {
                       superName = classPai.superName;
                    }
                    else {
                        is_parent = false;
                    }
                }
               
                
                // Debug
		//System.err.println("METHOD");
		//for (util.List<Statement> stmts = n.body; stmts != null; stmts = stmts.tail)
		//	System.err.println(stmts.toString());

		/* Issues the body instructions */
		for (util.List<Statement> stmts = n.body; stmts != null; stmts = stmts.tail) {
                        System.err.println("\nNode: " + n.getClass().getName() + " - Accepting statement");
			stmts.head.accept(this);
                        System.err.println("\nNode: " + n.getClass().getName() + " - Returning from statement");
		}

		/* Return */
                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting rValue");
                this.calledArgument = null;
		LlvmValue rValue = n.returnExp.accept(this);
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from rValue: " + rValue.toString());
                System.err.println("\nNode: " + n.getClass().getName() + " - Returning from rValue.type: " + rValue.type.toString());
                System.err.println("\nNode: " + n.getClass().getName() + " - n.returnType.toString(): " + n.returnType.toString());
                System.err.println("\nNode: " + n.getClass().getName() + " - mName: " + mName);
                System.err.println("\nNode: " + n.getClass().getName() + " - rValue.type.toString().contains(\"*\"): " + rValue.type.toString().contains("*"));
                if(this.calledArgument != null)
                System.err.println("\nNode: " + n.getClass().getName() + " - this.calledArgument: " + this.calledArgument);
                
                
                
                if(((this.calledArgument != null) && (this.calledArgument == "this"))) {
                    System.err.println("\nNode: " + n.getClass().getName() + " - TYPE 0.THIS RETURN TYPE");
                    System.err.println("\nNode: " + n.getClass().getName() + " - rValue.type.toString()" + rValue.type.toString());
                    LlvmType retType = new LlvmClassType(rValue.type.toString().substring(7));
                    LlvmValue ret = new LlvmNamedValue("%" + this.calledArgument, retType);
                    
                    assembler.add(new LlvmRet(ret));
                    this.calledArgument = null;
                }
                
                else if((rValue.type.toString().contains("%class.")) && (rValue.type.toString().contains("*")) && ((this.calledArgument != null)))
                {
                                    //
                
                    //List<LlvmValue> offsets = new LinkedList<LlvmValue>();
                    //offsets.add(new LlvmIntegerLiteral(0));
                    //offsets.add(new LlvmIntegerLiteral(0));
                    //LlvmRegister lhs = new LlvmRegister(new LlvmPointer(rValue.type));
                    //assembler.add(new LlvmGetElementPointer(lhs, rValue, offsets));
                    //assembler.add(new LlvmRet(lhs));
                    
                    /* if(n.returnType.toString().equals(rValue.type.toString())) {
                    LlvmType retType = new LlvmClassType(rValue.type.toString().substring(7));
                    LlvmValue ret = new LlvmNamedValue(this.calledArgument, new LlvmPointer(retType));
                    assembler.add(new LlvmRet(ret));
                    this.calledArgument = null; 
                    }
                    
                    else { */
                    System.err.println("\nNode: " + n.getClass().getName() + " - TYPE 1 RETURN TYPE");
                    LlvmType retType = new LlvmClassType(rValue.type.toString().substring(7));
                    LlvmValue ret = new LlvmNamedValue(this.calledArgument, new LlvmPointer(retType));
                    LlvmRegister retLoad = new LlvmRegister(retType);
                    assembler.add(new LlvmLoad(retLoad, ret));
                    
                    assembler.add(new LlvmRet(retLoad));
                    this.calledArgument = null;
                    //}
                
                }
                
                else {
                    //if((this.calledArgument != null) && (this.calledArgument == "this")) {
                        
                    //}
                    if(rValue.type.toString().contains(("%class."))) {
                        System.err.println("\nNode: " + n.getClass().getName() + " - TYPE 2 RETURN TYPE");
                        LlvmType retType = new LlvmClassType(rValue.type.toString().substring(7));
                        LlvmValue ret = new LlvmNamedValue(this.calledArgument, new LlvmPointer(retType));
                        assembler.add(new LlvmRet(ret));
                        this.calledArgument = null; 
                    }
                    else {
                        System.err.println("\nNode: " + n.getClass().getName() + " - TYPE 3 RETURN TYPE");
                        assembler.add(new LlvmRet(rValue));
                    }
                }
                this.calledArgument = null;
                
		/* Close the method */
		assembler.add(new LlvmCloseDefinition());

                System.err.println("\nNode: " + n.getClass().getName() + " - Accepting and returning n.returnType");
		return n.returnType.accept(this);
	}

	/* Minus node */
	public LlvmValue visit(Minus n) {

		System.err.println("Node: " + n.getClass().getName());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting v1");
		LlvmValue v1 = n.lhs.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from v1: " + v1.toString());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting v2");
		LlvmValue v2 = n.rhs.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from v2: " + v2.toString());
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmMinus(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;
	}

	/* NewArray node */
	public LlvmValue visit(NewArray n) {
		System.err.println("Node: " + n.getClass().getName());

		// Allocation and store of the new array
		LlvmValue array = new LlvmRegister(new LlvmArray(0,
				LlvmPrimitiveType.I32));
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting n.size");
		LlvmValue size = n.size.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from n.size");
		assembler.add(new LlvmMalloc(array, LlvmPrimitiveType.I32, size));
		array.type = new LlvmPointer(LlvmPrimitiveType.I32);
		assembler.add(new LlvmStore(LlvmMalloc.lastArraySize, array));

		return array;
	}

	/* NewObject node */
	public LlvmValue visit(NewObject n) {

		System.err.println("Node: " + n.getClass().getName());
                ClassNode ObjClass = symTab.classes.get(n.className.s);

		/* Issues the className identifier */
		LlvmRegister lhs = new LlvmRegister(new LlvmPointer(ObjClass.type));
                System.err.println("Node: " + n.getClass().getName() + " --- MALLOC : " + lhs.type.toString());
                
                if(ObjClass.isJoined == false) {
                    ObjClass.mList = joinMethods(n.className.s);
                    ObjClass.isJoined = true;
                }
                
                int size = (8 * ObjClass.mList.size()) + (ObjClass.varList.size()*8);
                
		assembler.add(new LlvmMalloc(lhs, lhs.type, "%class." + n.className.s, size));
                
                // Time to instanciate vTable
                initVtable(ObjClass.className, lhs, false, null);

		/* Return */
		return lhs;
	}
        
        public void initVtable(String className, LlvmValue lhs, boolean isFather, LlvmType extendType) {
            ClassNode ObjClass = symTab.classes.get(className);
            LlvmValue ObjRef = lhs;
            int counter;
            
            if(ObjClass.isExtended) {
                initVtable(ObjClass.superName, lhs, true, ObjClass.type);
            }
            
            if(isFather) {
                LlvmValue cast = new LlvmRegister(new LlvmPointer (ObjClass.type));
                assembler.add(new LlvmBitcast(cast, lhs, cast.type));
                ObjRef = cast;
            }
            
            LlvmValue vTable = new LlvmRegister(LlvmPrimitiveType.VTable);
            assembler.add(new LlvmBitcast(vTable, ObjRef, LlvmPrimitiveType.VTable));

            counter = 0;
            for (Map.Entry<String, MethodNode> entry : ObjClass.mList.entrySet()) {
                boolean is_on_father = false;
                ClassNode classPai;
                String nomePai = null;
                if(ObjClass.isExtended) {
                    nomePai = ObjClass.superName;
                    boolean more = true;
                    classPai = symTab.classes.get(nomePai);
                    while (more) {
                        for (Map.Entry<String, MethodNode> entry_p : classPai.mList.entrySet()) {
                            if(entry.getKey() == entry_p.getKey()) {
                                is_on_father = true;
                                more = false;
                                break;
                            }
                        }
                        if(more) {
                            if(classPai.isExtended) {
                                nomePai = classPai.superName;
                            }
                            else {
                                more = false;
                            }
                        }
                    }
                }
                    String classOrigin;
                    if(is_on_father) {
                        classOrigin = nomePai;
                    }
                    else {
                        classOrigin = className;
                    }
                
                    LlvmValue Function = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I8));
                    LlvmValue Conv = new LlvmNamedValue("@__" + entry.getValue().mName + "__" + classOrigin, new LlvmClassPointer(classOrigin, entry.getValue().rType, entry.getValue().fList));
                    assembler.add(new LlvmBitcast(Function, Conv, Function.type));


                    LlvmValue store_pointer = new LlvmRegister(LlvmPrimitiveType.VTable);

                    List<LlvmValue> offsets = new LinkedList<LlvmValue>();
                    offsets.add(new LlvmIntegerLiteral(0));
                    offsets.add(new LlvmIntegerLiteral(counter));
                    assembler.add(new LlvmGetElementPointer(store_pointer, vTable, offsets));
                    store_pointer.type = new LlvmPointer(new LlvmPointer(LlvmPrimitiveType.I8));
                    assembler.add(new LlvmStore(Function, store_pointer));
                    counter++;
            }
            
        }
	
	/* Not node */
	public LlvmValue visit(Not n) {

		System.err.println("Node: " + n.getClass().getName());
		
		/* Accepts the value of the expression */
		LlvmValue val = n.exp.accept(this);
		
		/* Register with the boolean value */
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		
		/* Issues the instruction */
		assembler.add(new LlvmMinus(lhs, LlvmPrimitiveType.I1, new LlvmBool(1), val));

		return lhs;
	}

	/* Plus node */
	public LlvmValue visit(Plus n) {

		System.err.println("Node: " + n.getClass().getName());

		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting v1");
		LlvmValue v1 = n.lhs.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from v1: " + v1.toString());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting v2");
		LlvmValue v2 = n.rhs.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from v2: " + v2.toString());
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmPlus(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;
	}

	/* Print node */
	public LlvmValue visit(Print n) {
		System.err.println("Node: " + n.getClass().getName());

		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting v");
		LlvmValue v = n.exp.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from v: " + v.toString());
		System.err.println("\nNode: " + n.getClass().getName() + " - v.type: "
				+ v.type);

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
		LlvmCall PrintCall = new LlvmCall(PrintReg, LlvmPrimitiveType.I32, pts,
				"@printf", args);
		System.err.println("Node: " + n.getClass().getName() + " - PrintReg : "
				+ PrintReg.toString());
		System.err.println("Node: " + n.getClass().getName() + " - LlvmCall : "
				+ PrintCall.toString());
		System.err.println("Node: " + n.getClass().getName() + " - Args : "
				+ args.toString());
		System.err.println("Node: " + n.getClass().getName() + " - Pts : "
				+ pts.toString());
		assembler.add(PrintCall);
		// assembler.add(new LlvmCall(new LlvmRegister(LlvmPrimitiveType.I32),
		// LlvmPrimitiveType.I32, pts, "@printf", args));
		return null;
	}

	/* This node */
	public LlvmValue visit(This n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Returns a register that points to the class */
		LlvmRegister lhs = new LlvmRegister(new LlvmPointer(new LlvmClassType(
				classEnv.className)));
		assembler.add(new LlvmLoad(lhs, new LlvmNamedValue("%this_tmp",
				new LlvmPointer(lhs.type))));
                
                this.calledArgument = "this";

		return lhs;
	}

	/* Times node */
	public LlvmValue visit(Times n) {

		System.err.println("Node: " + n.getClass().getName());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting v1");
		LlvmValue v1 = n.lhs.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from v1: " + v1.toString());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting v2");
		LlvmValue v2 = n.rhs.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from v2: " + v2.toString());
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmTimes(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;

	}

	/* True node */
	public LlvmValue visit(True n) {

		System.err.println("Node: " + n.getClass().getName());
		LlvmValue TrueValue = new LlvmBool(1);
		return TrueValue;
	}

	/* VarDecl node */
	public LlvmValue visit(VarDecl n) {

		System.err.println("Node: " + n.getClass().getName());
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting n.type and returning new LlvNamedValue");
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
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting cond");
		cond = n.condition.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from cond");
		assembler.add(new LlvmBranch(cond, new LlvmLabelValue("%" + wBegin),
				new LlvmLabelValue("%" + wEnd)));
		assembler.add(new LlvmLabel(new LlvmLabelValue(wBegin)));

		/* Insert the body of the loop */
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Accepting body");
		wBody.accept(this);
		System.err.println("\nNode: " + n.getClass().getName()
				+ " - Returning from cond");

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
        
        // Join method list with super(s)
        public LinkedHashMap<String, MethodNode> joinMethods(String className) {
            System.err.println("JoinList: " + className);
            ClassNode Node = symTab.classes.get(className);
            
            LinkedHashMap<String, MethodNode> newMethods;
            
            if(Node.isExtended == true) {
                newMethods = joinMethods(Node.superName);
            }
            else {
                newMethods = new LinkedHashMap<String, MethodNode>();
                newMethods.putAll(Node.mList);
                return newMethods;
            }
            
            for (Map.Entry<String, MethodNode> entry : Node.mList.entrySet())
            {
                newMethods.put(entry.getKey(), entry.getValue());
            }
            
            return newMethods;
        }
        
	public LlvmValue visit(ClassDeclExtends n) {

		System.err.println("Node: " + n.getClass().getName() + " - Current Class: " + n.name.toString());

		/* Get the actual class */
		classEnv = symTab.classes.get(n.name.toString());
                
                // Merge method list with super(s)
                System.err.println("Node: " + n.getClass().getName() + "Before Merging Methods : " + classEnv.mList.size());
                if(classEnv.isJoined == false) {
                    classEnv.mList = joinMethods(n.name.toString());
                    classEnv.isJoined = true;
                }
                System.err.println("Node: " + n.getClass().getName() + "After Merging Methods : " + classEnv.mList.size());

                // Adds vTable variable.
                String vTableName = "[" + classEnv.mList.size() + " x i8 *]";
                String attr;
                
                // Adds vTable variable.
                if(classEnv.classType.typeList.isEmpty()) {
                    attr = "type " + "{" + vTableName + "}";
                }
                else {
                    attr = "type " + "{" + vTableName + ", " + classEnv.classType.toString().substring(1);
                }
                
                System.err.println("Node: " + n.getClass().getName() + " - attr: " + attr);
                LlvmConstantDeclaration ClassDef = new LlvmConstantDeclaration("%class." + classEnv.className, attr);
                assembler.add(0, ClassDef);

		/* Deal with the instructions for the methods */
		for (util.List<MethodDecl> met = n.methodList; met != null; met = met.tail) {
                        System.err.println("\nNode: " + n.getClass().getName() + " - Accepting Method");
			met.head.accept(this);
                        System.err.println("\nNode: " + n.getClass().getName() + " - Returning from Method");
                }

		return null;
	}

}