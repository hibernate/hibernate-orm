/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: SubDetail.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;


public class SubDetail {
	private String name;
	private long id;
	/**
	 * Returns the id.
	 * @return long
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	
	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
}






