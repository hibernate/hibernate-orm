//$Id$
package org.hibernate.ejb.test.ops;

/**
 * @author Emmanuel Bernard
 */
public class Child {
	private String name;
	private int age;

	Child() {
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

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	private Parent parent;

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
