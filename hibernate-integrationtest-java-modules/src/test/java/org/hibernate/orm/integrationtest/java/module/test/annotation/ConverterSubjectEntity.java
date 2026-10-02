package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "ConverterSubjectEntity")
public class ConverterSubjectEntity {
	@Id
	public Long id;

	public StubConvertibleType convertible;
}
