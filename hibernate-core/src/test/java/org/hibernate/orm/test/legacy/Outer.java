/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.io.Serializable;

/**
 * @author Stefano Travelli
 */
public class Outer implements Serializable {
	private OuterKey id;
	private String bubu;

	public OuterKey getId() {
		return id;
	}

	public void setId(OuterKey id) {
		this.id = id;
	}

	public String getBubu() {
		return bubu;
	}

	public void setBubu(String bubu) {
		this.bubu = bubu;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Outer)) return false;

		final Outer cidDetail = (Outer) o;

		if (id != null ? !id.equals(cidDetail.id) : cidDetail.id != null) return false;

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}
}
