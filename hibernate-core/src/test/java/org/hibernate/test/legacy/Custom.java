/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Custom.java 7275 2005-06-22 18:58:16Z oneovthafew $
package org.hibernate.test.legacy;



public class Custom implements Cloneable {
	String id;
	private String name;
	
	public Object clone() {
		try {
			return super.clone();
		}
		catch (CloneNotSupportedException cnse) {
			throw new RuntimeException();
		}
	}

	void setName(String name) {
		this.name = name;
	}

	String getName() {
		return name;
	}

}






