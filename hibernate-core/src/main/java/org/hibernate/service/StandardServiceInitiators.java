/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.cfgxml.internal.CfgXmlAccessServiceInitiator;
import org.hibernate.boot.internal.DefaultSessionFactoryBuilderInitiator;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.bytecode.internal.ProxyFactoryFactoryInitiator;
import org.hibernate.cache.internal.RegionFactoryInitiator;
import org.hibernate.engine.config.internal.ConfigurationServiceInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.internal.MultiTenantConnectionProviderInitiator;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryInitiator;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jndi.internal.JndiServiceInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformResolverInitiator;
import org.hibernate.event.internal.EntityCopyObserverFactoryInitiator;
import org.hibernate.hql.internal.QueryTranslatorFactoryInitiator;
import org.hibernate.id.factory.internal.MutableIdentifierGeneratorFactoryInitiator;
import org.hibernate.jmx.internal.JmxServiceInitiator;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.internal.PersisterFactoryInitiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyResolverInitiator;
import org.hibernate.resource.beans.spi.ManagedBeanRegistryInitiator;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractorInitiator;
import org.hibernate.tool.schema.internal.SchemaManagementToolInitiator;

/**
 * Central definition of the standard set of service initiators defined by Hibernate.
 * 
 * @author Steve Ebersole
 */
public final class StandardServiceInitiators {
	private StandardServiceInitiators() {
	}

	public static final List<StandardServiceInitiator> LIST = buildStandardServiceInitiatorList();

	private static List<StandardServiceInitiator> buildStandardServiceInitiatorList() {
		final ArrayList<StandardServiceInitiator> serviceInitiators = new ArrayList<StandardServiceInitiator>();

		serviceInitiators.add( DefaultSessionFactoryBuilderInitiator.INSTANCE );

		serviceInitiators.add( BytecodeProviderInitiator.INSTANCE );
		serviceInitiators.add( ProxyFactoryFactoryInitiator.INSTANCE );

		serviceInitiators.add( CfgXmlAccessServiceInitiator.INSTANCE );
		serviceInitiators.add( ConfigurationServiceInitiator.INSTANCE );
		serviceInitiators.add( PropertyAccessStrategyResolverInitiator.INSTANCE );

		serviceInitiators.add( ImportSqlCommandExtractorInitiator.INSTANCE );
		serviceInitiators.add( SchemaManagementToolInitiator.INSTANCE );

		serviceInitiators.add( JdbcEnvironmentInitiator.INSTANCE );
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
		serviceInitiators.add( RefCursorSupportInitiator.INSTANCE );

		serviceInitiators.add( QueryTranslatorFactoryInitiator.INSTANCE );
		serviceInitiators.add( MutableIdentifierGeneratorFactoryInitiator.INSTANCE);

		serviceInitiators.add( JtaPlatformResolverInitiator.INSTANCE );
		serviceInitiators.add( JtaPlatformInitiator.INSTANCE );

		serviceInitiators.add( SessionFactoryServiceRegistryFactoryInitiator.INSTANCE );

		serviceInitiators.add( RegionFactoryInitiator.INSTANCE );

		serviceInitiators.add( TransactionCoordinatorBuilderInitiator.INSTANCE );

		serviceInitiators.add( ManagedBeanRegistryInitiator.INSTANCE );
		serviceInitiators.add( EntityCopyObserverFactoryInitiator.INSTANCE );

		serviceInitiators.trimToSize();

		return Collections.unmodifiableList( serviceInitiators );
	}
}
