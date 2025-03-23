/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.array;


/**
 * @author Emmanuel Bernard
 */
public class A {
	private Integer id;
	private B[] bs;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public B[] getBs() {
		return bs;
	}

	public void setBs(B[] bs) {
		this.bs = bs;
	}
}
