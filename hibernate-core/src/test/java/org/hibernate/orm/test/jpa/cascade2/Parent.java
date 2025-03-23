/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade2;


/**
 * todo: describe Parent
 *
 * @author Steve Ebersole
 */
public class Parent {
	private Long id;
	private String name;
	private ParentInfo info;

	public Parent() {
	}

	public Parent(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ParentInfo getInfo() {
		return info;
	}

	public void setInfo(ParentInfo info) {
		this.info = info;
	}
}
