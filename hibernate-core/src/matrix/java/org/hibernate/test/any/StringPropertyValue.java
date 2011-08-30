package org.hibernate.test.any;


/**
 * todo: describe StringPropertyValue
 *
 * @author Steve Ebersole
 */
public class StringPropertyValue implements PropertyValue {
	private Long id;
	private String value;

	public StringPropertyValue() {
	}

	public StringPropertyValue(String value) {
		this.value = value;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String asString() {
		return value;
	}
}
