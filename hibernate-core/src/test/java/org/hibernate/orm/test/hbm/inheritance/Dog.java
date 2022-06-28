package org.hibernate.orm.test.hbm.inheritance;

public class Dog extends Animal {

	private DogName name;

	public DogName getName() {
		return name;
	}

	public void setName(DogName name) {
		this.name = name;
	}
}
