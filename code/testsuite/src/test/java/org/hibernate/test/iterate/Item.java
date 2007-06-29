//$Id: Item.java 6593 2005-04-28 15:52:26Z oneovthafew $
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
