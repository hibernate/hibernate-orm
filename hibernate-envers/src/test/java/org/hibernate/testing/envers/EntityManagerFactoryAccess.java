/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.dialect.Dialect;

import org.hibernate.testing.orm.junit.DialectContext;

/**
 * Contract for things that expose an EntityManagerFactory
 *
 * @author Chris Cranford
 */
public interface EntityManagerFactoryAccess extends DialectAccess {
	EntityManagerFactory getEntityManagerFactory();

	@Override
	default Dialect getDialect() {
		return DialectContext.getDialect();
	}
}
