/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.envers;

import javax.persistence.EntityManagerFactory;

/**
 * Envers contract for something that can build an EntityManagerFactory based on an audit strategy.
 *
 * @author Chris Cranford
 */
public interface EnversEntityManagerFactoryProducer {
	EntityManagerFactory produceEntityManagerFactory(String auditStrategyName);
}
