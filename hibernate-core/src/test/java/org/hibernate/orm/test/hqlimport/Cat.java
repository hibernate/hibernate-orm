package org.hibernate.orm.test.hqlimport;

public class Cat extends Animal {
	private boolean indoor;

	public boolean isIndoor() {
		return indoor;
	}

	public void setIndoor(boolean indoor) {
		this.indoor = indoor;
	}
}
