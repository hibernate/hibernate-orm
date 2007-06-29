//$Id: Category.java 6957 2005-05-31 04:21:58Z oneovthafew $
package org.hibernate.test.joinfetch;

/**
 * @author Gavin King
 */
public class Category {
	
	private String name;

	Category() {}

	public Category(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
