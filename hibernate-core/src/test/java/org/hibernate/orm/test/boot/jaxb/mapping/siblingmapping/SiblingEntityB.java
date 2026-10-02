package org.hibernate.orm.test.boot.jaxb.mapping.siblingmapping;

public class SiblingEntityB extends SiblingBase {
	private int id;

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}
}
