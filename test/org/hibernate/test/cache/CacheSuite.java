package org.hibernate.test.cache;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.test.cache.treecache.optimistic.OptimisticTreeCacheTest;
import org.hibernate.test.cache.treecache.pessimistic.TreeCacheTest;
import org.hibernate.test.cache.ehcache.EhCacheTest;

/**
 * @author Steve Ebersole
 */
public class CacheSuite {

	public static Test suite() {
		TestSuite suite = new TestSuite( "CacheProvider tests");
		suite.addTest( OptimisticTreeCacheTest.suite() );
		suite.addTest( TreeCacheTest.suite() );
		suite.addTest( EhCacheTest.suite() );
		return suite;
	}
}
