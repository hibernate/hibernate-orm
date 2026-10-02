package org.hibernate.orm.test.boot.jaxb.mapping.siblingconflict;

public class ConflictEntityA extends ConflictBase {
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
