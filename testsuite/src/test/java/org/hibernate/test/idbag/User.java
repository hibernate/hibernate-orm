//$Id: User.java 6058 2005-03-11 17:05:19Z oneovthafew $
package org.hibernate.test.idbag;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gavin King
 */
public class User {
	private String name;
	private List groups = new ArrayList();
	
	User() {}
	
	public User(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	

	void setName(String name) {
		this.name = name;
	}

	public List getGroups() {
		return groups;
	}
	
	void setGroups(List groups) {
		this.groups = groups;
	}
	
}
