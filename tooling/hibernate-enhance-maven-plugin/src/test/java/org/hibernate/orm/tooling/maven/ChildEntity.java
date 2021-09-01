package org.hibernate.orm.tooling.maven;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class ChildEntity extends ParentEntity {

	String childValue;

}
