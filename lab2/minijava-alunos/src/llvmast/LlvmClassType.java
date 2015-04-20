package llvmast;

public class LlvmClassType extends LlvmType {
	private String type;

	public LlvmClassType(String name) {
		this.type = name;
	}

	@Override
	public String toString() {
		return "%class." + type;
	}
}
