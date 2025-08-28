/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.ids;

import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MulId implements Serializable {
	private Integer id1;

	private Integer id2;

	public MulId() {
	}

	public MulId(Integer id1, Integer id2) {
		this.id1 = id1;
		this.id2 = id2;
	}

	public Integer getId1() {
		return id1;
	}

	public void setId1(Integer id1) {
		this.id1 = id1;
	}

	public Integer getId2() {
		return id2;
	}

	public void setId2(Integer id2) {
		this.id2 = id2;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MulId) ) {
			return false;
		}

		MulId mulId = (MulId) o;

		if ( id1 != null ? !id1.equals( mulId.id1 ) : mulId.id1 != null ) {
			return false;
		}
		if ( id2 != null ? !id2.equals( mulId.id2 ) : mulId.id2 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id1 != null ? id1.hashCode() : 0);
		result = 31 * result + (id2 != null ? id2.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "MulId(" + id1 + ", " + id2 + ")";
	}
}
