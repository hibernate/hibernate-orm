/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.util.Properties;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.SessionFactoryBuilder;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.After;
import org.junit.Before;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class DualNodeTestCase extends BaseCoreFunctionalTestCase {
	private static final Log log = LogFactory.getLog( DualNodeTestCase.class );

	public static final String NODE_ID_PROP = "hibernate.test.cluster.node.id";
	public static final String NODE_ID_FIELD = "nodeId";
	public static final String LOCAL = "local";
	public static final String REMOTE = "remote";

	private SecondNodeEnvironment secondNodeEnvironment;

	@Override
	public String[] getMappings() {
		return new String[] {
				"cache/infinispan/functional/Contact.hbm.xml", "cache/infinispan/functional/Customer.hbm.xml"
		};
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}
	
	@Override
	protected void prepareStandardServiceRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		corePrepareStandardServiceRegistryBuilder( serviceRegistryBuilder );
		serviceRegistryBuilder.applySetting( NODE_ID_PROP, LOCAL );
		serviceRegistryBuilder.applySetting( NODE_ID_FIELD, LOCAL );
	}
	
	protected void corePrepareStandardServiceRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName() );
		serviceRegistryBuilder.applySetting( AvailableSettings.JTA_PLATFORM, getJtaPlatformClass().getName() );
		serviceRegistryBuilder.applySetting( Environment.TRANSACTION_STRATEGY, getTransactionFactoryClass().getName() );
		serviceRegistryBuilder.applySetting( Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName() );
		serviceRegistryBuilder.applySetting( Environment.USE_QUERY_CACHE, String.valueOf( getUseQueryCache() ) );
	}

	@Override
	protected void cleanupTest() throws Exception {
		cleanupTransactionManagement();
	}

	protected void cleanupTransactionManagement() {
		DualNodeJtaTransactionManagerImpl.cleanupTransactions();
		DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
		ClusterAwareRegionFactory.clearCacheManagers();
	}

	@Before
	public void prepare() throws Exception {
		secondNodeEnvironment = new SecondNodeEnvironment();
	}

	@After
	public void unPrepare() {
		if ( secondNodeEnvironment != null ) {
			secondNodeEnvironment.shutDown();
		}
	}

	protected SecondNodeEnvironment secondNodeEnvironment() {
		return secondNodeEnvironment;
	}

	protected Class getCacheRegionFactory() {
		return ClusterAwareRegionFactory.class;
	}

	protected Class getConnectionProviderClass() {
		return DualNodeConnectionProviderImpl.class;
	}

	protected Class getJtaPlatformClass() {
		return DualNodeJtaPlatformImpl.class;
	}

	protected Class getTransactionFactoryClass() {
		return CMTTransactionFactory.class;
	}

	protected void sleep(long ms) {
		try {
			Thread.sleep( ms );
		}
		catch ( InterruptedException e ) {
			log.warn( "Interrupted during sleep", e );
		}
	}

	protected boolean getUseQueryCache() {
		return true;
	}

	protected void configureSecondNode(StandardServiceRegistryBuilder builder) {

	}

	public class SecondNodeEnvironment {
		private StandardServiceRegistryBuilder serviceRegistryBuilder;
		private StandardServiceRegistryImpl serviceRegistry;
		private SessionFactoryImplementor sessionFactory;

		public SecondNodeEnvironment() {
			Properties properties = constructProperties();
			
			// TODO: Look into separating out some of these steps in
			// BaseCoreFuntionalTestCase
			BootstrapServiceRegistry bootstrapServiceRegistry = buildBootstrapServiceRegistry();
			serviceRegistryBuilder = new StandardServiceRegistryBuilder( bootstrapServiceRegistry )
					.applySettings( properties );
			corePrepareStandardServiceRegistryBuilder( serviceRegistryBuilder );
			serviceRegistryBuilder.applySetting( NODE_ID_PROP, REMOTE );
			serviceRegistryBuilder.applySetting( NODE_ID_FIELD, REMOTE );
			configureSecondNode( serviceRegistryBuilder );
			serviceRegistry = (StandardServiceRegistryImpl) serviceRegistryBuilder.build();
			
			MetadataBuilder metadataBuilder = getMetadataBuilder( bootstrapServiceRegistry, serviceRegistry );
			configMetadataBuilder(metadataBuilder);
			MetadataImplementor metadata = (MetadataImplementor)metadataBuilder.build();
			afterConstructAndConfigureMetadata( metadata );
			applyCacheSettings( metadata );
			SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
			configSessionFactoryBuilder(sessionFactoryBuilder);
			sessionFactory = ( SessionFactoryImplementor )sessionFactoryBuilder.build();
		}

		public StandardServiceRegistryBuilder getServiceRegistryBuilder() {
			return serviceRegistryBuilder;
		}

		public StandardServiceRegistryImpl getServiceRegistry() {
			return serviceRegistry;
		}

		public SessionFactoryImplementor getSessionFactory() {
			return sessionFactory;
		}

		public void shutDown() {
			if ( sessionFactory != null ) {
				try {
					sessionFactory.close();
					sessionFactory = null;
				}
				catch ( Exception ignore ) {
				}
			}
			if ( serviceRegistry != null ) {
				try {
					serviceRegistry.destroy();
					serviceRegistry = null;
				}
				catch ( Exception ignore ) {
				}
			}
		}
	}
}
