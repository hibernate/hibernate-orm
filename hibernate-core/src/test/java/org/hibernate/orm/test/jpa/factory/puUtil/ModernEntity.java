/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.factory.puUtil;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity(name = "ModernEntity")
public class ModernEntity {

	@Id
	@GeneratedValue
	private int id;

	private int foo;

	public ModernEntity() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getFoo() {
		return foo;
	}

	public void setFoo(int foo) {
		this.foo = foo;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ModernEntity that = (ModernEntity) o;

		return id == that.id;

	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public String toString() {
		return "ModernEntity{" +
				"id=" + id +
				'}';
	}
}
