//$Id: Organization.java 7547 2005-07-19 18:21:35Z oneovthafew $
package org.hibernate.test.sql.hand;

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
