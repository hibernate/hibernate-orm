package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "CompositeSubjectEntity")
public class CompositeSubjectEntity {
	@Id
	public Long id;

	public StubCompositeType composite;
}
