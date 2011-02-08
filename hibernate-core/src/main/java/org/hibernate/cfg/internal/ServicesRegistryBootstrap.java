/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.internal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.service.classloading.internal.ClassLoaderServiceInitiator;
import org.hibernate.service.internal.ServiceRegistryImpl;
import org.hibernate.service.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.jdbc.dialect.internal.DialectFactoryInitiator;
import org.hibernate.service.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.service.jmx.internal.JmxServiceInitiator;
import org.hibernate.service.jndi.internal.JndiServiceInitiator;
import org.hibernate.service.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.service.spi.ServiceInitiator;

/**
 * The standard bootstrap process for Hibernate services
 *
 * @author Steve Ebersole
 */
public class ServicesRegistryBootstrap {
	private List<ServiceInitiator> serviceInitiators = new ArrayList<ServiceInitiator>();

	public ServicesRegistryBootstrap() {
		serviceInitiators.add( ClassLoaderServiceInitiator.INSTANCE );
		serviceInitiators.add( JndiServiceInitiator.INSTANCE );
		serviceInitiators.add( JmxServiceInitiator.INSTANCE );

		serviceInitiators.add( ConnectionProviderInitiator.INSTANCE );
		serviceInitiators.add( DialectResolverInitiator.INSTANCE );
		serviceInitiators.add( DialectFactoryInitiator.INSTANCE );
		serviceInitiators.add( JdbcServicesInitiator.INSTANCE );

		serviceInitiators.add( JtaPlatformInitiator.INSTANCE );
		//serviceInitiators.add( TransactionFactoryInitiator.INSTANCE );
	}

	public ServiceRegistryImpl initiateServicesRegistry(Map configurationValues) {
		final ServiceRegistryImpl servicesRegistry = new ServiceRegistryImpl( serviceInitiators );
		servicesRegistry.initialize( configurationValues );
		return servicesRegistry;
	}
}
