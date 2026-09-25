package org.hibernate.orm.test.collection.lazynocascade;

/**
 * @author Vasily Kochnev
 */
public class Child extends BaseChild {
	private String name;

	/**
	 * @return Name of the child.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
}
