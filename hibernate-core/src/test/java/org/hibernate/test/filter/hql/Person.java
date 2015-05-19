/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.filter.hql;


/**
 * Base of inheritence hierarchy
 *
 * @author Steve Ebersole
 */
public class Person {
	private Long id;
	private String name;
	private char sex;

	/**
	 * Used by persistence
	 */
	protected Person() {
	}

	public Person(String name, char sex) {
		this.name = name;
		this.sex = sex;
	}

	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public char getSex() {
		return sex;
	}

	public void setSex(char sex) {
		this.sex = sex;
	}
}
