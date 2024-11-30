/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.target;



/**
 * @author Emmanuel Bernard
 */
public interface Owner {
	String getName();
	void setName(String name);
}
