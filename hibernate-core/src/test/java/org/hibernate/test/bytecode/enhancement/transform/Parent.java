package org.hibernate.test.bytecode.enhancement.transform;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Parent {
	@Id
	String id;
}
