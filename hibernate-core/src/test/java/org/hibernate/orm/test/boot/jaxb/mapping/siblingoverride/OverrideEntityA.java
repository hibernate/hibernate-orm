package org.hibernate.orm.test.boot.jaxb.mapping.siblingoverride;

public class OverrideEntityA extends OverrideBase {
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
