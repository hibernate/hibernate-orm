/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: User.java 7175 2005-06-17 05:23:15Z oneovthafew $
package org.hibernate.test.joinfetch;
import java.util.HashMap;
import java.util.Map;

public class User {
	private String name;
	private Map groups = new HashMap();

	public User(String name) {
		this.name = name;
	}

	User() {}

	public Map getGroups() {
		return groups;
	}

	public void setGroups(Map groups) {
		this.groups = groups;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
