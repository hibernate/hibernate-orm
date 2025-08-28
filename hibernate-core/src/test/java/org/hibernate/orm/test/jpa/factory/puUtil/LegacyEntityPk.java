/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.factory.puUtil;

import java.io.Serializable;

public class LegacyEntityPk implements Serializable {

	private int primitivePk1;

	private int primitivePk2;

	public LegacyEntityPk() {
	}

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LegacyEntityPk that = (LegacyEntityPk) o;

		if (primitivePk1 != that.primitivePk1) return false;
		return primitivePk2 == that.primitivePk2;

	}

	@Override
	public int hashCode() {
		int result = primitivePk1;
		result = 31 * result + primitivePk2;
		return result;
	}
}
