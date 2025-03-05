/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Up implements Serializable {

	private String id1;
	private long id2;

	public String getId1() {
		return id1;
	}

	public long getId2() {
		return id2;
	}

	public void setId1(String string) {
		id1 = string;
	}

	public void setId2(long l) {
		id2 = l;
	}

	public boolean equals(Object other) {
		if ( !(other instanceof Up) ) return false;
		Up that = (Up) other;
		return this.id1.equals(that.id1) && this.id2==that.id2;
	}
	public int hashCode() {
		return id1.hashCode();
	}

}
