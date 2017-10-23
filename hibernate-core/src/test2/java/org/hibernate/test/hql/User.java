/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: User.java 5891 2005-02-24 01:18:15Z oneovthafew $
package org.hibernate.test.hql;
import java.util.List;

/**
 * @author Gavin King
 */
public class User {
	private Long id;
	private String userName;
	private Human human;
	private List permissions;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Human getHuman() {
		return human;
	}

	public void setHuman(Human human) {
		this.human = human;
	}

	public List getPermissions() {
		return permissions;
	}
	

	public void setPermissions(List permissions) {
		this.permissions = permissions;
	}
	
}
