/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Remove;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.SessionFactoryAccess;

/**
 * @author Steve Ebersole
 *
 * @apiNote This contract will be removed in 9.0.
 */
@Remove
public interface SessionFactoryServiceInitiatorContext {
	@Nonnull
	SessionFactoryAccess getSessionFactoryAccess();

	@Nonnull
	default SessionFactoryImplementor getSessionFactory() {
		return getSessionFactoryAccess().getSessionFactory();
	}

	@Nonnull
	SessionFactoryOptions getSessionFactoryOptions();

	@Nonnull
	ServiceRegistryImplementor getServiceRegistry();
}
