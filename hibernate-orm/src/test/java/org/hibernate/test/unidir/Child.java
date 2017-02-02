/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Child.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.unidir;



/**
 * @author Gavin King
 */
public class Child {
	private String name;
	private int age;

	Child() {
	}

	public Child(String name) {
		this( name, 0 );
	}

	public Child(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
