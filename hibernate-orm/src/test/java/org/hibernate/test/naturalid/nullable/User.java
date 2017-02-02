/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: User.java 6900 2005-05-25 01:24:22Z oneovthafew $
package org.hibernate.test.naturalid.nullable;


/**
 * @author Gavin King
 */
public class User {
	private Long id;
	private String name;
	private String org;
	private String password;
	private int intVal;

	User() {}

	public User(String name, String org, String password) {
		this.name = name;
		this.org = org;
		this.password = password;
	}

	public Long getId() {
		return id;
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

	public void setPassword(String password) {
		this.password = password;
	}

	public int getIntVal() {
		return intVal;
	}

	public void setIntVal(int intVal) {
		this.intVal = intVal;
	}
}
