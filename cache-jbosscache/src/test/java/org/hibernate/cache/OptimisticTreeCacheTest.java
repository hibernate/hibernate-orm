/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cache;

import junit.framework.Test;
import junit.framework.Assert;
import org.jboss.cache.Fqn;
import org.jboss.cache.TreeCache;
import org.jboss.cache.config.Option;
import org.jboss.cache.optimistic.DataVersion;

import org.hibernate.cache.impl.bridge.RegionFactoryCacheProviderBridge;
import org.hibernate.cfg.Environment;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.cache.BaseCacheProviderTestCase;
import org.hibernate.test.tm.SimpleJtaTransactionManagerImpl;

/**
 * @author Steve Ebersole
 */
public class OptimisticTreeCacheTest extends BaseCacheProviderTestCase {
	public OptimisticTreeCacheTest(String x) {
		super( x );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OptimisticTreeCacheTest.class );
	}

	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}

	protected Class getCacheProvider() {
		return OptimisticTreeCacheProvider.class;
	}

	protected String getConfigResourceKey() {
		return Environment.CACHE_PROVIDER_CONFIG;
	}

	protected String getConfigResourceLocation() {
		return "treecache-optimistic.xml";
	}

	protected boolean useTransactionManager() {
		return true;
	}

	public void testCacheLevelStaleWritesFail() throws Throwable {
		Fqn fqn = new Fqn( "whatever" );
		TreeCache treeCache = ( ( OptimisticTreeCacheProvider ) ( ( RegionFactoryCacheProviderBridge ) sfi().getSettings().getRegionFactory() ).getCacheProvider() ).getUnderlyingCache();

		Long long1 = new Long(1);
		Long long2 = new Long(2);

		try {
			System.out.println( "****************************************************************" );
			SimpleJtaTransactionManagerImpl.getInstance().begin();
			treeCache.put( fqn, "ITEM", long1, ManualDataVersion.gen( 1 ) );
			SimpleJtaTransactionManagerImpl.getInstance().commit();

			System.out.println( "****************************************************************" );
			SimpleJtaTransactionManagerImpl.getInstance().begin();
			treeCache.put( fqn, "ITEM", long2, ManualDataVersion.gen( 2 ) );
			SimpleJtaTransactionManagerImpl.getInstance().commit();

			try {
				System.out.println( "****************************************************************" );
				SimpleJtaTransactionManagerImpl.getInstance().begin();
				treeCache.put( fqn, "ITEM", long1, ManualDataVersion.gen( 1 ) );
				SimpleJtaTransactionManagerImpl.getInstance().commit();
				Assert.fail( "stale write allowed" );
			}
			catch( Throwable ignore ) {
				// expected behavior
				SimpleJtaTransactionManagerImpl.getInstance().rollback();
			}

			Long current = ( Long ) treeCache.get( fqn, "ITEM" );
			Assert.assertEquals( "unexpected current value", 2, current.longValue() );
		}
		finally {
			try {
				treeCache.remove( fqn, "ITEM" );
			}
			catch( Throwable ignore ) {
			}
		}
	}

	private static class ManualDataVersion implements DataVersion {
		private final int version;

		public ManualDataVersion(int version) {
			this.version = version;
		}

		public boolean newerThan(DataVersion dataVersion) {
			return this.version > ( ( ManualDataVersion ) dataVersion ).version;
		}

		public static Option gen(int version) {
			ManualDataVersion mdv = new ManualDataVersion( version );
			Option option = new Option();
			option.setDataVersion( mdv );
			return option;
		}
	}
}
