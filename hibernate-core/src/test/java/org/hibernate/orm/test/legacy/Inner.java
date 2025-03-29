/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.io.Serializable;
import java.util.List;

/**
 * @author Stefano Travelli
 */
public class Inner implements Serializable {
	private InnerKey id;
	private String dudu;
	private List middles;
	private Outer backOut;

	public InnerKey getId() {
		return id;
	}

	public void setId(InnerKey id) {
		this.id = id;
	}

	public String getDudu() {
		return dudu;
	}

	public void setDudu(String dudu) {
		this.dudu = dudu;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Inner)) return false;

		final Inner cidSuper = (Inner) o;

		if (id != null ? !id.equals(cidSuper.id) : cidSuper.id != null) return false;

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}
	public List getMiddles() {
		return middles;
	}

	public void setMiddles(List list) {
		middles = list;
	}

	public Outer getBackOut() {
		return backOut;
	}

	public void setBackOut(Outer outer) {
		backOut = outer;
	}

}
