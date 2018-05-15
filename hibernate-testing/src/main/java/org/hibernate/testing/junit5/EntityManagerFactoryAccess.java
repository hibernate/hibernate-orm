/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5;

import javax.persistence.EntityManagerFactory;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract for things that expose an EntityManagerFactory
 *
 * @author Chris Cranford
 */
public interface EntityManagerFactoryAccess extends DialectAccess {
	EntityManagerFactory getEntityManagerFactory();

	@Override
	default Dialect getDialect() {
		return getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getJdbcServices().getDialect();
	}
}
