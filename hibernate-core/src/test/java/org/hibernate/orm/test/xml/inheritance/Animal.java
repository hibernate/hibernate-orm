package org.hibernate.orm.test.xml.inheritance;

public abstract class Animal {

	private long id;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}


}
