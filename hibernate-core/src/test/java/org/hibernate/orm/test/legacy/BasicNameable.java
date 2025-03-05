/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 * @author administrator
 *
 *
 */
public class BasicNameable implements Nameable {

	private String name;
	private Long id;

	/**
	 * @see Nameable#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see Nameable#setName()
	 */
	public void setName(String n) {
		name = n;
	}

	/**
	 * @see Nameable#getKey()
	 */
	public Long getKey() {
		return id;
	}

	/**
	 * @see Nameable#setKey()
	 */
	public void setKey(Long k) {
		id = k;
	}

}
