/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util.dtd;


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
