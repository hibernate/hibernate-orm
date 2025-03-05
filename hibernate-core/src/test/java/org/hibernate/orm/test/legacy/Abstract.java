/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.util.Set;

public abstract class Abstract extends Foo implements AbstractProxy {

	private java.sql.Time time;
	private Set abstracts;

	public java.sql.Time getTime() {
		return time;
	}

	public void setTime(java.sql.Time time) {
		this.time = time;
	}

	public Set getAbstracts() {
		return abstracts;
	}

	public void setAbstracts(Set abstracts) {
		this.abstracts = abstracts;
	}

}
