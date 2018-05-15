/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util.dtd;


/**
 * The Child class.
 *
 * @author Steve Ebersole
 */
public class Child {
	private Long id;
	private int age;
	private Parent parent;

	public Child() {
	}

	public Long getId() {
		return id;
	}

	public Parent getParent() {
		return parent;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}


	/*package*/ void injectParent(Parent parent) {
		this.parent = parent;
	}
}
