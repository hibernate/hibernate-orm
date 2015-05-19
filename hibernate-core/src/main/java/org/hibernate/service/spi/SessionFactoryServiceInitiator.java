/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;

/**
 * Contract for an initiator of services that target the specialized service registry
 * {@link SessionFactoryServiceRegistry}
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceInitiator<R extends Service> extends ServiceInitiator<R>{
	/**
	 * Initiates the managed service.
	 * <p/>
	 * Note for implementors: signature is guaranteed to change once redesign of SessionFactory building is complete
	 *
	 * @param sessionFactory The session factory.  Note the the session factory is still in flux; care needs to be taken
	 * in regards to what you call.
	 * @param sessionFactoryOptions Options specified for building the SessionFactory
	 * @param registry The service registry.  Can be used to locate services needed to fulfill initiation.
	 *
	 * @return The initiated service.
	 */
	public R initiateService(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions,
			ServiceRegistryImplementor registry);

}
