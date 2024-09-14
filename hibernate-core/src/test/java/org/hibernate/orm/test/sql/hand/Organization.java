/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.hand;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Gavin King
 */
public class Organization {
	private long id;
	private String name;
	private Collection employments;

	public Organization(String name) {
		this.name = name;
		employments = new HashSet();
	}

	public Organization() {}

	/**
	 * @return Returns the employments.
	 */
	public Collection getEmployments() {
		return employments;
	}
	/**
	 * @param employments The employments to set.
	 */
	public void setEmployments(Collection employments) {
		this.employments = employments;
	}
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

}
