//$Id: CacheTest.java 11398 2007-04-10 14:54:07Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.ReadWriteCache;
import org.hibernate.cache.HashtableCacheProvider;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.testing.junit.UnitTestCase;

public class CacheTest extends UnitTestCase {

	public CacheTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new TestSuite( CacheTest.class );
	}

	public void testCaches() throws Exception {
		doTestCache( new HashtableCacheProvider() );
	}

	public void doTestCache(CacheProvider cacheProvider) throws Exception {

		Cache cache = cacheProvider.buildCache( String.class.getName(), System.getProperties() );

		long longBefore = cache.nextTimestamp();

		Thread.sleep(15);

		long before = cache.nextTimestamp();

		Thread.sleep(15);

		//cache.setTimeout(1000);
		CacheConcurrencyStrategy ccs = new ReadWriteCache();
		ccs.setCache(cache);

		// cache something

		assertTrue( ccs.put("foo", "foo", before, null, null, false) );

		Thread.sleep(15);

		long after = cache.nextTimestamp();

		assertTrue( ccs.get("foo", longBefore)==null );
		assertTrue( ccs.get("foo", after).equals("foo") );

		assertTrue( !ccs.put("foo", "foo", before, null, null, false) );

		// update it:

		SoftLock lock = ccs.lock("foo", null);

		assertTrue( ccs.get("foo", after)==null );
		assertTrue( ccs.get("foo", longBefore)==null );

		assertTrue( !ccs.put("foo", "foo", before, null, null, false) );

		Thread.sleep(15);

		long whileLocked = cache.nextTimestamp();

		assertTrue( !ccs.put("foo", "foo", whileLocked, null, null, false) );

		Thread.sleep(15);

		ccs.release("foo", lock);

		assertTrue( ccs.get("foo", after)==null );
		assertTrue( ccs.get("foo", longBefore)==null );

		assertTrue( !ccs.put("foo", "bar", whileLocked, null, null, false) );
		assertTrue( !ccs.put("foo", "bar", after, null, null, false) );

		Thread.sleep(15);

		long longAfter = cache.nextTimestamp();

		assertTrue( ccs.put("foo", "baz", longAfter, null, null, false) );

		assertTrue( ccs.get("foo", after)==null );
		assertTrue( ccs.get("foo", whileLocked)==null );

		Thread.sleep(15);

		long longLongAfter = cache.nextTimestamp();

		assertTrue( ccs.get("foo", longLongAfter).equals("baz") );

		// update it again, with multiple locks:

		SoftLock lock1 = ccs.lock("foo", null);
		SoftLock lock2 = ccs.lock("foo", null);

		assertTrue( ccs.get("foo", longLongAfter)==null );

		Thread.sleep(15);

		whileLocked = cache.nextTimestamp();

		assertTrue( !ccs.put("foo", "foo", whileLocked, null, null, false) );

		Thread.sleep(15);

		ccs.release("foo", lock2);

		Thread.sleep(15);

		long betweenReleases = cache.nextTimestamp();

		assertTrue( !ccs.put("foo", "bar", betweenReleases, null, null, false) );
		assertTrue( ccs.get("foo", betweenReleases)==null );

		Thread.sleep(15);

		ccs.release("foo", lock1);

		assertTrue( !ccs.put("foo", "bar", whileLocked, null, null, false) );

		Thread.sleep(15);

		longAfter = cache.nextTimestamp();

		assertTrue( ccs.put("foo", "baz", longAfter, null, null, false) );
		assertTrue( ccs.get("foo", whileLocked)==null );

		Thread.sleep(15);

		longLongAfter = cache.nextTimestamp();

		assertTrue( ccs.get("foo", longLongAfter).equals("baz") );

	}

}
