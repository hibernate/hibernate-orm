package org.hibernate.orm.tooling.maven;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class ChildEntity extends ParentEntity {

	String childValue;

}
