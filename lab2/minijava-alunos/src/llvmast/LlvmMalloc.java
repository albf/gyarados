package llvmast;
public  class LlvmMalloc extends LlvmInstruction{
    public LlvmValue lhs;
    public LlvmType type;
    public LlvmValue nElements;
    
    // Armazena o tamanho do ��ltimo objeto. 
    // ��til para pegar o Lenght do vetor 
    public static LlvmValue lastArraySize = null;
    public static LlvmValue lastArraySizeReg = null;
    
    private int size; 
    private LlvmRegister lhsTimes;
    private LlvmRegister lhsCall;
    
	String times, call, bitcast;

	/**
	 * 
	 * Construtor Malloc: recebe apenas o tamanho em bytes que se deseja alocar
	 * Cabe a voc�� calcular qual ser�� esse tamanho
	 * 
	 * @param lhs
	 * @param size
	 */
	public LlvmMalloc(LlvmValue lhs, LlvmValue size){
		call = new String();
		bitcast = new String();

		LlvmValue lhsCall = new  LlvmRegister(LlvmPrimitiveType.I8);

		// Malloc de <size> bytes
		call = "  " + lhsCall + " = call i8* @malloc ( i32 "+ size + ")\n";  
		bitcast = "  " + lhs + " = bitcast i8* " + lhsCall + " to i32*\n";
	}
	
	
	/**
	 * Construtor para Alocar objetos de Classe: recebe o tipo (que deve ser LlvmStructure)
	 * e o nome da Classe (objName)
	 *  
	 * @param lhs
	 * @param type
	 * @param className
	 */
	public LlvmMalloc(LlvmValue lhs, LlvmType type, String className){
		MallocImpl(lhs, type, new LlvmIntegerLiteral(1), className);
	}

	
	/**
	 * Construtor para Alocar Vetor de Inteiros: recebe o tipo (que deve ser I32)
	 * e o numero de elementos
	 * 
	 * @param lhs
	 * @param type
	 * @param nElements
	 */
	
	public LlvmMalloc(LlvmValue lhs, LlvmType type, LlvmValue nElements){
		MallocImpl(lhs, type, nElements, null);
	}
	
	
	/**
	 *  Implementa����o
	 *  
	 */
	private void MallocImpl(LlvmValue lhs, LlvmType type, LlvmValue nElements, String className){
		this.lhs = lhs;
		this.type = type;
		this.nElements = nElements;
		this.size = 0;
		LlvmMalloc.lastArraySize = null;
		
		// calculando o tamanho do malloc (em Bytes)
		if ( type instanceof LlvmStructure ){
			size = ((LlvmStructure) type).sizeByte;
		} else {
			this.nElements = null;
			if ( type == LlvmPrimitiveType.I32 ){
				size = 4;
				LlvmMalloc.lastArraySize = nElements;
			} else { 
				// Se �� um bool
				size = 1;
			}
		}		
		
		lhsTimes = new LlvmRegister(LlvmPrimitiveType.I32);
		LlvmRegister lhsPlus = new LlvmRegister(LlvmPrimitiveType.I32);
		LlvmMalloc.lastArraySizeReg = nElements;
		lhsCall = new  LlvmRegister(LlvmPrimitiveType.I8);
		
		times = new String("  " + lhsTimes + " = mul i32 " + size + ", " + nElements + "\n"+
						   "  " + lhsPlus  + " = add i32 " + size + ", " + lhsTimes + "\n");
		call = new String("  " + lhsCall + " = call i8* @malloc ( i32 "+ lhsPlus + ")\n");
		if (className == null)
			bitcast = new String("  " + lhs + " = bitcast i8* " + lhsCall + " to " + type + "*");
		else
			bitcast = new String("  " + lhs + " = bitcast i8* " + lhsCall + " to " + className + "*");
	}    
	
    public String toString(){
    	if(times == null) return call + bitcast;
    	return times + call  + bitcast;
    }
}
	
