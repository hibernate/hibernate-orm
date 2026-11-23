package org.hibernate.orm.test.boot.jaxb.mapping.cascade;

public class Child {
	private Integer id;

	public Child() {
	}

	public Child(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}
