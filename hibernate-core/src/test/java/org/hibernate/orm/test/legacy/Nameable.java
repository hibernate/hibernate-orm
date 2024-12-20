/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;


/**
 *
 */
public interface Nameable {
	public String getName();
	public void setName(String name);
	public Long getKey();
	public void setKey(Long key);
}
