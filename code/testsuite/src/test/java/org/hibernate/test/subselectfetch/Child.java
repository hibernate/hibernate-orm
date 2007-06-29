//$Id: Child.java 6095 2005-03-17 05:57:29Z oneovthafew $
package org.hibernate.test.subselectfetch;

import java.util.List;

/**
 * @author Gavin King
 */
public class Child {
	private String name;
	private List friends;

	Child() {}
	public Child(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	public List getFriends() {
		return friends;
	}
	
	public void setFriends(List friends) {
		this.friends = friends;
	}
	
	
}
