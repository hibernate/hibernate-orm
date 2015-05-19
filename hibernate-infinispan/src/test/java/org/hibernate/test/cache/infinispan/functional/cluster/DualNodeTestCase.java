/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class DualNodeTestCase extends BaseNonConfigCoreFunctionalTestCase {
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
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		applyStandardSettings( settings );

		settings.put( NODE_ID_PROP, LOCAL );
		settings.put( NODE_ID_FIELD, LOCAL );
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

	protected Class<? extends TransactionCoordinatorBuilder> getTransactionCoordinatorBuilder() {
		return JtaTransactionCoordinatorBuilderImpl.class;
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

	protected void configureSecondNode(StandardServiceRegistryBuilder ssrb) {
	}

	@SuppressWarnings("unchecked")
	protected void applyStandardSettings(Map settings) {
		settings.put( Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName() );
		settings.put( AvailableSettings.JTA_PLATFORM, getJtaPlatformClass().getName() );
		settings.put( Environment.TRANSACTION_COORDINATOR_STRATEGY, getTransactionCoordinatorBuilder().getName() );
		settings.put( Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName() );
		settings.put( Environment.USE_QUERY_CACHE, String.valueOf( getUseQueryCache() ) );
	}

	public class SecondNodeEnvironment {
		private StandardServiceRegistry serviceRegistry;
		private SessionFactoryImplementor sessionFactory;

		public SecondNodeEnvironment() {
			StandardServiceRegistryBuilder ssrb = constructStandardServiceRegistryBuilder();
			applyStandardSettings( ssrb.getSettings() );
			ssrb.applySetting( NODE_ID_PROP, REMOTE );
			ssrb.applySetting( NODE_ID_FIELD, REMOTE );
			configureSecondNode( ssrb );

			serviceRegistry = ssrb.build();

			MetadataSources metadataSources = new MetadataSources( serviceRegistry );
			applyMetadataSources( metadataSources );

			Metadata metadata = metadataSources.buildMetadata();
			applyCacheSettings( metadata );
			afterMetadataBuilt( metadata );

			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
		}

		public StandardServiceRegistry getServiceRegistry() {
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
					StandardServiceRegistryBuilder.destroy( serviceRegistry );
				}
				catch (Exception ignore) {
				}
			}
		}
	}
}
