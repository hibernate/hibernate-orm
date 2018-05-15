/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: I.java 7203 2005-06-19 02:01:05Z oneovthafew $
package org.hibernate.test.legacy;


/**
 * @author Gavin King
 */
public class I {
	private Long id;
	private String name;
	private char type;
	private K parent;
	
	public K getParent() {
		return parent;
	}
	
	public void setParent(K parent) {
		this.parent = parent;
	}

	void setType(char type) {
		this.type = type;
	}

	char getType() {
		return type;
	}

	void setName(String name) {
		this.name = name;
	}

	String getName() {
		return name;
	}
}
