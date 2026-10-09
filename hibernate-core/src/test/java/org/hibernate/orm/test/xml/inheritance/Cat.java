package org.hibernate.orm.test.xml.inheritance;

public class Cat extends Animal {

	private CatName name;

	public CatName getName() {
		return name;
	}

	public void setName(CatName name) {
		this.name = name;
	}
}
