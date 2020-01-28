package org.hibernate.jpamodelgen.test.usertype.jpa;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class EntityWithSimpleTypes {
	
	@Id
	private Long id;

	private Integer integerValue;
	
	private ConcreteSimpleType concreteSimpleType;

	private ConcreteSerializableSimpleType concreteSerializableSimpleType;

	private ConcreteSerializableSimpleTypeImplementingSerializable concreteSerializableSimpleTypeImplementingSerializable;

}
