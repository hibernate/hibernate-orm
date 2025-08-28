/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import jakarta.persistence.EntityManagerFactory;

/**
 * Contract for something that can build a SessionFactory.
 *
 * Used by SessionFactoryScopeExtension to create the
 * SessionFactoryScope.
 *
 * Generally speaking, a test class would implement SessionFactoryScopeContainer
 * and return the SessionFactoryProducer to be used for those tests.
 * The SessionFactoryProducer is then used to build the SessionFactoryScope
 * which is injected back into the SessionFactoryScopeContainer
 *
 * @see EntityManagerFactoryExtension
 * @see EntityManagerFactoryScope
 *
 * @author Steve Ebersole
 */
public interface EntityManagerFactoryProducer {
	EntityManagerFactory produceEntityManagerFactory();
}
