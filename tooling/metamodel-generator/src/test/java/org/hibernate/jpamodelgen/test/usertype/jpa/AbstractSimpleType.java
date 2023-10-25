package org.hibernate.jpamodelgen.test.usertype.jpa;

public abstract class AbstractSimpleType {

	private Integer value;
		
	public AbstractSimpleType(Integer value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value == null ? "" : value.toString();
	}
		
}
