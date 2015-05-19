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
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;

/**
 * Contract for builder of {@link SessionFactoryServiceRegistry} instances.
 * <p/>
 * Is itself a service within the standard service registry.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceRegistryFactory extends Service {
	/**
	 * Create the registry.
	 *
	 * @param sessionFactory The (still being built) session factory.  Generally this is useful
	 * for grabbing a reference for later use.  However, care should be taken when invoking on
	 * the session factory until after it has been fully initialized.
	 * @param sessionFactoryOptions The build options.
	 *
	 * @return The registry
	 */
	public SessionFactoryServiceRegistryImpl buildServiceRegistry(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions);

}
