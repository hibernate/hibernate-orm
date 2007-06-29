//$Id$
package org.hibernate.test.iterate;

/**
 * @author Gavin King
 */
public class Item {
	private String name;
	Item() {}
	public Item(String n) {
		name = n;
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
