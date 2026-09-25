package org.hibernate.orm.test.bytecode.enhancement.optimizer;

import jakarta.persistence.Entity;

@Entity(name = "ChildEntity9")
public class ChildEntity9 extends AncestorEntity {
	private String childField;

	public String getChildField() {
		return childField;
	}

	public void setChildField(String childField) {
		this.childField = childField;
	}
}
