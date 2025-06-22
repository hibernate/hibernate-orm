/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.model;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class MyEntity {
	private Long id;
	private String name;
	private String surname;
	private MyEntity other;

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

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public MyEntity getOther() {
		return other;
	}

	public void setOther(MyEntity other) {
		this.other = other;
	}
}
