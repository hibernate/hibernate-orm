/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.internal;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class SessionFactoryServiceRegistryImpl extends AbstractServiceRegistryImpl implements SessionFactoryServiceRegistry  {

	private final SessionFactoryOptions sessionFactoryOptions;
	private final SessionFactoryImplementor sessionFactory;

	@SuppressWarnings( {"unchecked"})
	public SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions) {
		super( parent );

		this.sessionFactory = sessionFactory;
		this.sessionFactoryOptions = sessionFactoryOptions;

		// for now, just use the standard initiator list
		for ( SessionFactoryServiceInitiator initiator : StandardSessionFactoryServiceInitiators.LIST ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator );
		}
	}

	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		SessionFactoryServiceInitiator<R> sessionFactoryServiceInitiator = (SessionFactoryServiceInitiator<R>) serviceInitiator;
		return sessionFactoryServiceInitiator.initiateService( sessionFactory, sessionFactoryOptions, this );
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		//TODO nothing to do here or should we inject SessionFactory properties?
	}
}
