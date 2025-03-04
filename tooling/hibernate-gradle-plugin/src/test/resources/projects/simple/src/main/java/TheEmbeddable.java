import jakarta.persistence.Embeddable;

@Embeddable
public class TheEmbeddable {
	private String valueOne;
	private String valueTwo;

	public String getValueOne() {
		return valueOne;
	}

	public void setValueOne(String valueOne) {
		this.valueOne = valueOne;
	}

	public String getValueTwo() {
		return valueTwo;
	}

	public void setValueTwo(String valueTwo) {
		this.valueTwo = valueTwo;
	}
}
