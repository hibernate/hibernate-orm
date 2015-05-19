/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.util.Map;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.cache.infinispan.tm.JtaPlatformImpl;
import org.junit.Before;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class SingleNodeTestCase extends BaseNonConfigCoreFunctionalTestCase {
	private static final Log log = LogFactory.getLog( SingleNodeTestCase.class );
	protected TransactionManager tm;

	@Before
	public void prepare() {
		tm = getTransactionManager();
	}

	protected TransactionManager getTransactionManager() {
		try {
			Class<? extends JtaPlatform> jtaPlatformClass = getJtaPlatform();
			if ( jtaPlatformClass == null ) {
				return null;
			}
			else {
				return jtaPlatformClass.newInstance().retrieveTransactionManager();
			}
		}
		catch (Exception e) {
			log.error( "Error", e );
			throw new RuntimeException( e );
		}
	}

	@Override
	public String[] getMappings() {
		return new String[] {
				"cache/infinispan/functional/Item.hbm.xml",
				"cache/infinispan/functional/Customer.hbm.xml",
				"cache/infinispan/functional/Contact.hbm.xml"
		};
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}

	protected Class<? extends RegionFactory> getCacheRegionFactory() {
		return TestInfinispanRegionFactory.class;
	}

	protected Class<? extends TransactionCoordinatorBuilder> getTransactionCoordinatorBuilder() {
		return JtaTransactionCoordinatorBuilderImpl.class;
	}

	protected Class<? extends ConnectionProvider> getConnectionProviderClass() {
		return org.hibernate.test.cache.infinispan.tm.XaConnectionProvider.class;
	}

	protected Class<? extends JtaPlatform> getJtaPlatform() {
		return JtaPlatformImpl.class;
	}

	protected boolean getUseQueryCache() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( Environment.GENERATE_STATISTICS, "true" );
		settings.put( Environment.USE_QUERY_CACHE, String.valueOf( getUseQueryCache() ) );
		settings.put( Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName() );

		if ( getJtaPlatform() != null ) {
			settings.put( AvailableSettings.JTA_PLATFORM, getJtaPlatform() );
		}
		settings.put( Environment.TRANSACTION_COORDINATOR_STRATEGY, getTransactionCoordinatorBuilder().getName() );
		settings.put( Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName() );
	}

	protected void beginTx() throws Exception {
		tm.begin();
	}

	protected void setRollbackOnlyTx() throws Exception {
		tm.setRollbackOnly();
	}

	protected void setRollbackOnlyTx(Exception e) throws Exception {
		log.error( "Error", e );
		tm.setRollbackOnly();
		throw e;
	}

	protected void setRollbackOnlyTxExpected(Exception e) throws Exception {
		log.debug( "Expected behaivour", e );
		tm.setRollbackOnly();
	}

	protected void commitOrRollbackTx() throws Exception {
		if ( tm.getStatus() == Status.STATUS_ACTIVE ) {
			tm.commit();
		}
		else {
			tm.rollback();
		}
	}

   public static class TestInfinispanRegionFactory extends InfinispanRegionFactory {

      public TestInfinispanRegionFactory() {
         super(); // For reflection-based instantiation
      }

      @Override
      protected EmbeddedCacheManager createCacheManager(ConfigurationBuilderHolder holder) {
         return TestCacheManagerFactory.createClusteredCacheManager(holder);
      }

   }

}