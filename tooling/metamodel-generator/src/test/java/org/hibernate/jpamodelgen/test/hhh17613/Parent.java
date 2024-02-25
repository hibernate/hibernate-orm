package org.hibernate.jpamodelgen.test.hhh17613;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Parent {

	@Id
	private Long id;
}
