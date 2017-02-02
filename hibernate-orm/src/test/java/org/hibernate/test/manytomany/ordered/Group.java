/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomany.ordered;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Group implements Serializable {

	private Long id;
	private String org;
	private String name;
	private String description;

	private List users = new ArrayList();

	public Group() {
	}

	public Group(String name, String org) {
		this.org = org;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public List getUsers() {
		return users;
	}

	public void setUsers(List users) {
		this.users = users;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void addUser(User user) {
		if ( user.getGroups().add( this ) ) {
			getUsers().add( user );
		}
	}
}
