package org.hibernate.test.converter.literal;

/**
 * @author Janario Oliveira
 */
public class StringWrapper {
	private final String value;

	public StringWrapper(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
