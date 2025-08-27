/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interfaceproxy;


/**
 * @author Gavin King
 */
public interface Item {
	/**
	 * @return Returns the id.
	 */
	public Long getId();

	/**
	 * @return Returns the name.
	 */
	public String getName();

	/**
	 * @param name The name to set.
	 */
	public void setName(String name);
}
