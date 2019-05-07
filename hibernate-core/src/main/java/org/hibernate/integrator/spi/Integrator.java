/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.integrator.spi;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Contract for stuff that integrates with Hibernate.
 * <p/>
 * IMPL NOTE: called during session factory initialization (constructor), so not all parts of the passed session factory
 * will be available.
 *
 * @author Steve Ebersole
 * @since 4.0
 */
public interface Integrator {

	/**
	 * Perform integration.
	 *
	 * @param metadata The boot-time representation of the mapping information
	 * @param sessionFactory The session factory being created
	 * @param serviceRegistry The session factory's service registry
	 *
	 * @deprecated (since 6.0) Use {@link #integrate(Metadata, BootstrapContext, SessionFactoryImplementor, SessionFactoryServiceRegistry)}
	 * instead
	 */
	@Deprecated
	default void integrate(
			Metadata metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		throw new UnsupportedOperationException( "Unexpected call to Integrator#integrate(Metadata,SessionFactoryImplementor,SessionFactoryServiceRegistry)" );
	}

	/**
	 * Perform integration.
	 *
	 * @param metadata The boot-time representation of the mapping information
	 * @param bootstrapContext The BootstrapContext for the
	 * @param sessionFactory The session factory being created
	 * @param serviceRegistry The session factory's service registry
	 *
	 * @since 6.0
	 */
	default void integrate(
			Metadata metadata,
			BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		// simply call the legacy one, keeping implementors bytecode compatible.
		integrate( metadata, sessionFactory, serviceRegistry );
	}

	/**
	 * Tongue-in-cheek name for a shutdown callback.
	 *
	 * @param sessionFactory The session factory being closed.
	 * @param serviceRegistry That session factory's service registry
	 */
	void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry);

}
