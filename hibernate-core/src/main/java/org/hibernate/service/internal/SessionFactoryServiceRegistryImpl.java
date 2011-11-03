/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.service.internal;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
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

	// for now we need to hold on to the Configuration... :(
	private final Configuration configuration;
	private final MetadataImplementor metadata;
	private final SessionFactoryImplementor sessionFactory;

	@SuppressWarnings( {"unchecked"})
	public SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			SessionFactoryImplementor sessionFactory,
			Configuration configuration) {
		super( parent );

		this.sessionFactory = sessionFactory;
		this.configuration = configuration;
		this.metadata = null;

		// for now, just use the standard initiator list
		for ( SessionFactoryServiceInitiator initiator : StandardSessionFactoryServiceInitiators.LIST ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator );
		}
	}

	@SuppressWarnings( {"unchecked"})
	public SessionFactoryServiceRegistryImpl(
			ServiceRegistryImplementor parent,
			SessionFactoryImplementor sessionFactory,
			MetadataImplementor metadata) {
		super( parent );

		this.sessionFactory = sessionFactory;
		this.configuration = null;
		this.metadata = metadata;

		// for now, just use the standard initiator list
		for ( SessionFactoryServiceInitiator initiator : StandardSessionFactoryServiceInitiators.LIST ) {
			// create the bindings up front to help identify to which registry services belong
			createServiceBinding( initiator );
		}
	}

	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		// todo : add check/error for unexpected initiator types?
		SessionFactoryServiceInitiator<R> sessionFactoryServiceInitiator =
				(SessionFactoryServiceInitiator<R>) serviceInitiator;
		if ( metadata != null ) {
			return sessionFactoryServiceInitiator.initiateService( sessionFactory, metadata, this );
		}
		else if ( configuration != null ) {
			return sessionFactoryServiceInitiator.initiateService( sessionFactory, configuration, this );
		}
		else {
			throw new IllegalStateException( "Both metadata and configuration are null." );
		}
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		//TODO nothing to do here or should we inject SessionFactory properties?
	}
}
