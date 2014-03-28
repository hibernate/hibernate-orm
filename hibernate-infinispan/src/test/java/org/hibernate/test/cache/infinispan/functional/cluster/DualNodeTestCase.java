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

import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.metamodel.MetadataSources;
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
	public void configure(Configuration cfg) {
		standardConfigure( cfg );
		cfg.setProperty( NODE_ID_PROP, LOCAL );
		cfg.setProperty( NODE_ID_FIELD, LOCAL );
	}

	@Override
	protected void cleanupTest() throws Exception {
		cleanupTransactionManagement();
	}

	protected void cleanupTransactionManagement() {
		DualNodeJtaTransactionManagerImpl.cleanupTransactions();
		DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
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
		catch (InterruptedException e) {
			log.warn( "Interrupted during sleep", e );
		}
	}

	protected boolean getUseQueryCache() {
		return true;
	}

	protected void configureSecondNode(Configuration cfg) {

	}

	protected void standardConfigure(Configuration cfg) {
		super.configure( cfg );

		cfg.setProperty( Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName() );
		cfg.setProperty( AvailableSettings.JTA_PLATFORM, getJtaPlatformClass().getName() );
		cfg.setProperty( Environment.TRANSACTION_STRATEGY, getTransactionFactoryClass().getName() );
		cfg.setProperty( Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName() );
		cfg.setProperty( Environment.USE_QUERY_CACHE, String.valueOf( getUseQueryCache() ) );
	}

	public class SecondNodeEnvironment {
		private Configuration configuration;
		private StandardServiceRegistryImpl serviceRegistry;
		private SessionFactoryImplementor sessionFactory;

		public SecondNodeEnvironment() {
			configuration = constructConfiguration();
			standardConfigure( configuration );
			configuration.setProperty( NODE_ID_PROP, REMOTE );
			configuration.setProperty( NODE_ID_FIELD, REMOTE );
			configureSecondNode( configuration );
			afterConfigurationBuilt( configuration );
			serviceRegistry = buildServiceRegistry( buildBootstrapServiceRegistry(), configuration );
			MetadataSources sources = new MetadataSources( serviceRegistry.getParentServiceRegistry() );
			addMappings( sources );
			MetadataImplementor metadata = (MetadataImplementor) sources.getMetadataBuilder( serviceRegistry ).build();
			applyCacheSettings( metadata );
			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
		}

		public Configuration getConfiguration() {
			return configuration;
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
				}
				catch (Exception ignore) {
				}
			}
			if ( serviceRegistry != null ) {
				try {
					serviceRegistry.destroy();
				}
				catch (Exception ignore) {
				}
			}
		}
	}
}
