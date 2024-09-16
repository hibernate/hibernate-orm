/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;


/**
 * @author Emmanuel Bernard
 */
public interface Length<Type> {
	Type getLength();
}
