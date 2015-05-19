/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
