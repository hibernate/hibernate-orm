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
package org.hibernate.test.cache.infinispan.functional;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.Before;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.internal.jta.CMTTransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.test.cache.infinispan.tm.JtaPlatformImpl;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class SingleNodeTestCase extends BaseCoreFunctionalTestCase {
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

	protected Class<? extends TransactionFactory> getTransactionFactoryClass() {
		return CMTTransactionFactory.class;
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
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, String.valueOf( getUseQueryCache() ) );
		cfg.setProperty( Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName() );

		if ( getJtaPlatform() != null ) {
			cfg.getProperties().put( AvailableSettings.JTA_PLATFORM, getJtaPlatform() );
		}
		cfg.setProperty( Environment.TRANSACTION_STRATEGY, getTransactionFactoryClass().getName() );
		cfg.setProperty( Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName() );
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