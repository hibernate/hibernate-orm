package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "TypeSubjectEntity")
public class TypeSubjectEntity {
	@Id
	public Long id;

	public StubBasicType basic;
}
