/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


public class Z {

	private long id;
	private W w;

	/**
	 *
	 */
	public Z() {
	}


	/**
	 * @return
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return
	 */
	public W getW() {
		return w;
	}

	/**
	 * @param l
	 */
	public void setId(long l) {
		id = l;
	}

	/**
	 * @param w
	 */
	public void setW(W w) {
		this.w = w;
	}

}
