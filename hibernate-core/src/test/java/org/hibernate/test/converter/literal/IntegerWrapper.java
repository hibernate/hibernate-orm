package org.hibernate.test.converter.literal;

/**
 * @author Janario Oliveira
 */
public class IntegerWrapper {
	private final int value;

	public IntegerWrapper(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
