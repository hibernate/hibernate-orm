//$Id: Group.java 7175 2005-06-17 05:23:15Z oneovthafew $
package org.hibernate.test.joinfetch;
import java.util.HashMap;
import java.util.Map;

public class Group {
	private String name;
	private Map users = new HashMap();
	
	public Group(String name) {
		this.name = name;
	}

	Group() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map getUsers() {
		return users;
	}

	public void setUsers(Map users) {
		this.users = users;
	}

}
