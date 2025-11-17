/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.factory.puUtil;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

@Entity(name = "LegacyEntity")
@IdClass(LegacyEntityPk.class)
public class LegacyEntity {

	@Id
	private int primitivePk1;

	@Id
	private int primitivePk2;

	private String foo;

	public LegacyEntity() {}

	public int getPrimitivePk1() {
		return primitivePk1;
	}

	public void setPrimitivePk1(int primitivePk1) {
		this.primitivePk1 = primitivePk1;
	}

	public int getPrimitivePk2() {
		return primitivePk2;
	}

	public void setPrimitivePk2(int primitivePk2) {
		this.primitivePk2 = primitivePk2;
	}

	public String getFoo() {
		return foo;
	}

	public void setFoo(String foo) {
		this.foo = foo;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LegacyEntity that = (LegacyEntity) o;

		if (primitivePk1 != that.primitivePk1) return false;
		return primitivePk2 == that.primitivePk2;

	}

	@Override
	public int hashCode() {
		int result = primitivePk1;
		result = 31 * result + primitivePk2;
		return result;
	}

	@Override
	public String toString() {
		return "LegacyEntity{" +
				"primitivePk1=" + primitivePk1 +
				", primitivePk2=" + primitivePk2 +
				'}';
	}
}
