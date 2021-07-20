package org.hibernate.jpamodelgen.test.usertype.jpa;

import java.io.Serializable;

public class AbstractSerializableSimpleType implements Serializable {
	
	private Integer value;
	
	public AbstractSerializableSimpleType(Integer value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return value == null ? "" : value.toString();
	}
	
}
