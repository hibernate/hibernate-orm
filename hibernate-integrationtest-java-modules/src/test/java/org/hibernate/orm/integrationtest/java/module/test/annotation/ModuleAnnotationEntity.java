package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "ModuleAnnotationEntity")
public class ModuleAnnotationEntity {
	@Id
	public Long id;
}
