/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;

public class A {
	private Long id;
	private String name;
	private E forward;

	public A() {
	}

	public A(Long id) {
		this.id = id;
	}

	/**
	 * Returns the id.
	 * @return Long
	 */
	public Long getId() {
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
	public void setId(Long id) {
		this.id = id;
	}
	
	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public E getForward() {
		return forward;
	}

	public void setForward(E e) {
		forward = e;
	}

}






