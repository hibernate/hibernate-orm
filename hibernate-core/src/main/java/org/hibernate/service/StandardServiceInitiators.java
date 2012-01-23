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
package org.hibernate.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.cache.internal.RegionFactoryInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.transaction.internal.TransactionFactoryInitiator;
import org.hibernate.id.factory.internal.MutableIdentifierGeneratorFactoryInitiator;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.service.config.internal.ConfigurationServiceInitiator;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.service.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.service.jdbc.dialect.internal.DialectFactoryInitiator;
import org.hibernate.service.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.service.jmx.internal.JmxServiceInitiator;
import org.hibernate.service.jndi.internal.JndiServiceInitiator;
import org.hibernate.service.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractorInitiator;

/**
 * Central definition of the standard set of service initiators defined by Hibernate.
 * 
 * @author Steve Ebersole
 */
public class StandardServiceInitiators {
	public static List<BasicServiceInitiator> LIST = buildStandardServiceInitiatorList();

	private static List<BasicServiceInitiator> buildStandardServiceInitiatorList() {
		final List<BasicServiceInitiator> serviceInitiators = new ArrayList<BasicServiceInitiator>();

		serviceInitiators.add( ConfigurationServiceInitiator.INSTANCE );
		serviceInitiators.add( ImportSqlCommandExtractorInitiator.INSTANCE );

		serviceInitiators.add( JndiServiceInitiator.INSTANCE );
		serviceInitiators.add( JmxServiceInitiator.INSTANCE );

		serviceInitiators.add( PersisterClassResolverInitiator.INSTANCE );
		serviceInitiators.add( PersisterFactoryInitiator.INSTANCE );

		serviceInitiators.add( ConnectionProviderInitiator.INSTANCE );
		serviceInitiators.add( MultiTenantConnectionProviderInitiator.INSTANCE );
		serviceInitiators.add( DialectResolverInitiator.INSTANCE );
		serviceInitiators.add( DialectFactoryInitiator.INSTANCE );
		serviceInitiators.add( BatchBuilderInitiator.INSTANCE );
		serviceInitiators.add( JdbcServicesInitiator.INSTANCE );

		serviceInitiators.add( MutableIdentifierGeneratorFactoryInitiator.INSTANCE);

		serviceInitiators.add( JtaPlatformInitiator.INSTANCE );
		serviceInitiators.add( TransactionFactoryInitiator.INSTANCE );

		serviceInitiators.add( SessionFactoryServiceRegistryFactoryInitiator.INSTANCE );

		serviceInitiators.add( RegionFactoryInitiator.INSTANCE );

		return Collections.unmodifiableList( serviceInitiators );
	}
}
