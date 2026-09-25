package org.hibernate.orm.test.bytecode.enhancement.optimizer;

import jakarta.persistence.Entity;

@Entity(name = "ChildEntity6")
public class ChildEntity6 extends AncestorEntity {
	private String childField;

	public String getChildField() {
		return childField;
	}

	public void setChieldField(String childField) {
		this.childField = childField;
	}
}
