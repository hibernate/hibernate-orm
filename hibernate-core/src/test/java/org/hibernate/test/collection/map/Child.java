package org.hibernate.test.collection.map;


/**
 * todo: describe Child
 *
 * @author Steve Ebersole
 */
public class Child {
	private String name;
	private Parent parent;

	public Child() {
	}

	public Child(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
