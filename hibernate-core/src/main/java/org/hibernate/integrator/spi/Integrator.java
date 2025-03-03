/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.integrator.spi;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Contract for extensions that integrate with Hibernate.
 * <p>
 * The best way to make an implementation of {@code Integrator} available to Hibernate
 * is by making it discoverable via the standard Java {@link java.util.ServiceLoader}
 * facility.
 *
 * @implNote {@link #integrate(Metadata, BootstrapContext, SessionFactoryImplementor)}
 *           is called during the process of {@linkplain SessionFactoryImplementor
 *           session factory} initialization. In fact, it's called directly from the
 *           constructor of {@link org.hibernate.internal.SessionFactoryImpl}. So the
 *           passed session factory is not yet fully-initialized and is in a very
 *           fragile state.
 *
 * @author Steve Ebersole
 * @since 4.0
 */
@JavaServiceLoadable
public interface Integrator {

	/**
	 * Perform integration.
	 *
	 * @param metadata The fully initialized boot-time mapping model
	 * @param bootstrapContext The context for bootstrapping of the SessionFactory
	 * @param sessionFactory The SessionFactory being created
	 */
	default void integrate(
			Metadata metadata,
			BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
	}

	/**
	 * Tongue-in-cheek name for a shutdown callback.
	 *
	 * @param sessionFactory The session factory being closed.
	 * @param serviceRegistry That session factory's service registry
	 */
	default void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// do nothing by default
	}

}
