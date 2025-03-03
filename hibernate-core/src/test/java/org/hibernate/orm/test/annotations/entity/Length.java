/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;


/**
 * @author Emmanuel Bernard
 */
public interface Length<Type> {
	Type getLength();
}
