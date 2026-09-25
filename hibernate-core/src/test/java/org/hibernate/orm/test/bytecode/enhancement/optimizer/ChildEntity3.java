package org.hibernate.orm.test.bytecode.enhancement.optimizer;

import jakarta.persistence.Entity;

@Entity(name = "ChildEntity3")
public class ChildEntity3 extends AncestorEntity {
	private String childField;

	public String getChildField() {
		return childField;
	}

	public void setChieldField(String childField) {
		this.childField = childField;
	}
}
