package org.hibernate.orm.tooling.maven;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class TestEntity extends ChildEntity {

	@Id
	long id;

	String testValue;

}
