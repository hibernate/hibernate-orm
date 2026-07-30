/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.integrator.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Contract for extensions that integrate with Hibernate.
 * <p>
 * The best way to make an implementation of {@code Integrator} available to Hibernate
 * is by making it discoverable via the standard Java {@link java.util.ServiceLoader}
 * facility.
 *
 * @implNote {@link #integrate(Metadata, Context, SessionFactoryImplementor)}
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
	 * @param context The integration-time context
	 * @param sessionFactory The SessionFactory being created
	 *
	 * @since 8.0
	 */
	default void integrate(
			Metadata metadata,
			Context context,
			SessionFactoryImplementor sessionFactory) {
		integrate( metadata, context.getBootstrapContext(), sessionFactory );
	}

	/**
	 * Perform integration.
	 *
	 * @param metadata The fully initialized boot-time mapping model
	 * @param bootstrapContext The context for bootstrapping of the SessionFactory
	 * @param sessionFactory The SessionFactory being created
	 *
	 * @deprecated Use {@link #integrate(Metadata, Context, SessionFactoryImplementor)}.
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	default void integrate(
			Metadata metadata,
			BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
	}

	/**
	 * Tongue-in-cheek name for a shutdown callback.
	 *
	 * @param sessionFactory The session factory being closed.
	 * @param context The integration-time context
	 *
	 * @since 8.0
	 */
	default void disintegrate(SessionFactoryImplementor sessionFactory, Context context) {
		disintegrate( sessionFactory, (SessionFactoryServiceRegistry) sessionFactory.getServiceRegistry() );
	}

	/**
	 * Tongue-in-cheek name for a shutdown callback.
	 *
	 * @param sessionFactory The session factory being closed.
	 * @param serviceRegistry That session factory's service registry
	 *
	 * @deprecated Use {@link #disintegrate(SessionFactoryImplementor, Context)}.
	 */
	@Deprecated(since = "8.0", forRemoval = true)
	default void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// do nothing by default
	}

	/**
	 * Inputs which are valid during SessionFactory integration but are not
	 * represented by the finalized metadata or the in-flight SessionFactory.
	 *
	 * @since 8.0
	 */
	@Incubating
	interface Context {
		/**
		 * Access to managed beans using the bootstrap-time CDI access policy.
		 */
		default ManagedBeanRegistry getManagedBeanRegistry() {
			return getBootstrapContext().getManagedBeanRegistry();
		}

		/**
		 * @deprecated Access to the bootstrap context will be removed.
		 */
		@Deprecated(since = "8.0", forRemoval = true)
		BootstrapContext getBootstrapContext();
	}
}
