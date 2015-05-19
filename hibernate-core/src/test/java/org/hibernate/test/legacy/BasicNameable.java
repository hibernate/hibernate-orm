/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: BasicNameable.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;


/**
 * @author administrator
 *
 *
 */
public class BasicNameable implements Nameable {
	
	private String name;
	private Long id;
	
	/**
	 * @see Nameable#getName()
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @see Nameable#setName()
	 */
	public void setName(String n) {
		name = n;
	}
	
	/**
	 * @see Nameable#getKey()
	 */
	public Long getKey() {
		return id;
	}
	
	/**
	 * @see Nameable#setKey()
	 */
	public void setKey(Long k) {
		id = k;
	}
	
}






