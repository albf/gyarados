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
		for (LlvmInstruction instr : codeGenerator.assembler)
			r += instr + "\n";
		return r;
	}

	public LlvmValue visit(Program n) {

		System.err.println("Node: " + n.getClass().getName());

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

	public LlvmValue visit(ClassDeclSimple n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Get the method declaration list */

		return null;
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

	/* Block node */
	public LlvmValue visit(Block n) {

		System.err.println("Node: " + n.getClass().getName());

		/* Call accept in each Statement of the Block */
		util.List<Statement> stmts = n.body;
		while (stmts != null) {
			stmts.head.accept(this); // Accept the head
			stmts = stmts.tail; // Change to the tail
		}
		return null;
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

	/* Times node */
	public LlvmValue visit(Times n) {

		System.err.println("Node: " + n.getClass().getName());

		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmTimes(lhs, LlvmPrimitiveType.I32, v1, v2));
		return lhs;

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

	/* LessThan node */
	public LlvmValue visit(LessThan n) {

		System.err.println("Node: " + n.getClass().getName());

		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		assembler.add(new LlvmIcmp(lhs, 1, v1.type, v1, v2));
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

	public LlvmValue visit(IntegerLiteral n) {

		System.err.println("Node: " + n.getClass().getName());

		return new LlvmIntegerLiteral(n.value);
	};

	// Todos os visit's que devem ser implementados
	public LlvmValue visit(ClassDeclExtends n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(VarDecl n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(MethodDecl n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(Formal n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(IntArrayType n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(IntegerType n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(IdentifierType n) {

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

	public LlvmValue visit(Call n) {

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

	public LlvmValue visit(IdentifierExp n) {

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

	public LlvmValue visit(NewObject n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(Not n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}

	public LlvmValue visit(Identifier n) {

		System.err.println("Node: " + n.getClass().getName());

		return null;
	}
}
