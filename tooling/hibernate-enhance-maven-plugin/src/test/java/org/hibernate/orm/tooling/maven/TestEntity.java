package org.hibernate.orm.tooling.maven;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class TestEntity extends ChildEntity {

	@Id
	long id;

	String testValue;

}
