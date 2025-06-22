/*
 * SPDX-License-Identifier: Apache-2.0
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
