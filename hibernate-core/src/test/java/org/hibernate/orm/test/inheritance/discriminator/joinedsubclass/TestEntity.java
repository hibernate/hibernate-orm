/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

/**
 * @author Andrea Boriero
 */
public interface TestEntity {
	Integer getId();
	void setId(Integer id);
}
