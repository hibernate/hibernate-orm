package org.hibernate.jpamodelgen.test.usertype.jpa;

import java.io.Serializable;

public class ConcreteSerializableSimpleTypeImplementingSerializable extends ConcreteSerializableSimpleType implements Serializable {

	public ConcreteSerializableSimpleTypeImplementingSerializable(Integer value) {
		super( value );
	}
}
