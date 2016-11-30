/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;


import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

import org.hibernate.test.cache.infinispan.functional.AbstractFunctionalTest;
import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.hibernate.test.cache.infinispan.util.TxUtil;
import org.junit.ClassRule;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class DualNodeTest extends AbstractFunctionalTest {

	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( DualNodeTest.class );

	@ClassRule
	public static final InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

	public static final String REGION_FACTORY_DELEGATE = "hibernate.cache.region.factory_delegate";
	public static final String NODE_ID_PROP = "hibernate.test.cluster.node.id";
	public static final String NODE_ID_FIELD = "nodeId";
	public static final String LOCAL = "local";
	public static final String REMOTE = "remote";

	private SecondNodeEnvironment secondNodeEnvironment;

	protected void withTxSession(SessionFactory sessionFactory, TxUtil.ThrowingConsumer<Session, Exception> consumer) throws Exception {
		TxUtil.withTxSession(useJta, sessionFactory, consumer);
	}

	protected <T> T withTxSessionApply(SessionFactory sessionFactory, TxUtil.ThrowingFunction<Session, T, Exception> consumer) throws Exception {
		return TxUtil.withTxSessionApply(useJta, sessionFactory, consumer);
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"cache/infinispan/functional/entities/Contact.hbm.xml",
				"cache/infinispan/functional/entities/Customer.hbm.xml"
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		applyStandardSettings( settings );

		settings.put( NODE_ID_PROP, LOCAL );
		settings.put( NODE_ID_FIELD, LOCAL );
		settings.put( REGION_FACTORY_DELEGATE, getRegionFactoryClass() );
	}

	@Override
	protected void cleanupTest() throws Exception {
		cleanupTransactionManagement();
	}

	protected void cleanupTransactionManagement() {
		DualNodeJtaTransactionManagerImpl.cleanupTransactions();
		DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
	}

	@Override
	public void startUp() {
		super.startUp();
		// In some cases tests are multi-threaded, so they have to join the group
		infinispanTestIdentifier.joinContext();
		secondNodeEnvironment = new SecondNodeEnvironment();
	}

	@Override
	public void shutDown() {
		if ( secondNodeEnvironment != null ) {
			secondNodeEnvironment.shutDown();
		}
		super.shutDown();
	}

	protected SecondNodeEnvironment secondNodeEnvironment() {
		return secondNodeEnvironment;
	}

	protected Class getCacheRegionFactory() {
		return ClusterAwareRegionFactory.class;
	}

	protected Class getJtaPlatformClass() {
		return DualNodeJtaPlatformImpl.class;
	}

	protected Class<? extends TransactionCoordinatorBuilder> getTransactionCoordinatorBuilder() {
		return JtaTransactionCoordinatorBuilderImpl.class;
	}

	protected void configureSecondNode(StandardServiceRegistryBuilder ssrb) {
	}

	@SuppressWarnings("unchecked")
	protected void applyStandardSettings(Map settings) {
		settings.put( Environment.CACHE_REGION_FACTORY, ClusterAwareRegionFactory.class.getName() );
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
