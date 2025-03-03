/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Defines the context needed to call the {@link TypeSafeActivator}.
 *
 * @author Steve Ebersole
 */
public interface ActivationContext {
	/**
	 * Access the requested validation mode(s).
	 *
	 * @implNote The legacy code allowed multiple mode values to be specified, so that is why it is multivalued here.
	 * However, I cannot find any good reasoning why it was defined that way and even JPA states it should be a single
	 * value. For 4.1 (in maintenance) I think it makes the most sense to not mess with it.  Discuss for 4.2 and beyond.
	 *
	 * @return The requested validation modes
	 */
	Set<ValidationMode> getValidationModes();

	/**
	 * Access the mapping metadata
	 *
	 * @return The mapping metadata
	 */
	Metadata getMetadata();

	/**
	 * Access the SessionFactory being built to trigger this BV activation
	 *
	 * @return The SessionFactory being built
	 */
	SessionFactoryImplementor getSessionFactory();

	/**
	 * Access the ServiceRegistry specific to the SessionFactory being built.
	 *
	 * @return The SessionFactoryServiceRegistry
	 */
	SessionFactoryServiceRegistry getServiceRegistry();
}
