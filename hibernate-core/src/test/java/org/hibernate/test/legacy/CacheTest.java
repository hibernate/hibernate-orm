/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.legacy;

import org.hibernate.cache.spi.Cache;
import org.hibernate.cache.spi.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.CacheProvider;
import org.hibernate.cache.internal.HashtableCacheProvider;
import org.hibernate.cache.spi.ReadWriteCache;
import org.hibernate.cache.spi.access.SoftLock;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertTrue;

public class CacheTest extends BaseUnitTestCase {
	@Test
	public void testCaches() throws Exception {
		doTestCache( new HashtableCacheProvider() );
	}

	public void doTestCache(CacheProvider cacheProvider) throws Exception {

		Cache cache = cacheProvider.buildCache( String.class.getName(), System.getProperties() );

		long longBefore = cache.nextTimestamp();

		Thread.sleep( 15 );

		long before = cache.nextTimestamp();

		Thread.sleep( 15 );

		//cache.setTimeout(1000);
		CacheConcurrencyStrategy ccs = new ReadWriteCache();
		ccs.setCache( cache );

		// cache something

		assertTrue( ccs.put( "foo", "foo", before, null, null, false ) );

		Thread.sleep( 15 );

		long after = cache.nextTimestamp();

		assertTrue( ccs.get( "foo", longBefore ) == null );
		assertTrue( ccs.get( "foo", after ).equals( "foo" ) );

		assertTrue( !ccs.put( "foo", "foo", before, null, null, false ) );

		// update it:

		SoftLock lock = ccs.lock( "foo", null );

		assertTrue( ccs.get( "foo", after ) == null );
		assertTrue( ccs.get( "foo", longBefore ) == null );

		assertTrue( !ccs.put( "foo", "foo", before, null, null, false ) );

		Thread.sleep( 15 );

		long whileLocked = cache.nextTimestamp();

		assertTrue( !ccs.put( "foo", "foo", whileLocked, null, null, false ) );

		Thread.sleep( 15 );

		ccs.release( "foo", lock );

		assertTrue( ccs.get( "foo", after ) == null );
		assertTrue( ccs.get( "foo", longBefore ) == null );

		assertTrue( !ccs.put( "foo", "bar", whileLocked, null, null, false ) );
		assertTrue( !ccs.put( "foo", "bar", after, null, null, false ) );

		Thread.sleep( 15 );

		long longAfter = cache.nextTimestamp();

		assertTrue( ccs.put( "foo", "baz", longAfter, null, null, false ) );

		assertTrue( ccs.get( "foo", after ) == null );
		assertTrue( ccs.get( "foo", whileLocked ) == null );

		Thread.sleep( 15 );

		long longLongAfter = cache.nextTimestamp();

		assertTrue( ccs.get( "foo", longLongAfter ).equals( "baz" ) );

		// update it again, with multiple locks:

		SoftLock lock1 = ccs.lock( "foo", null );
		SoftLock lock2 = ccs.lock( "foo", null );

		assertTrue( ccs.get( "foo", longLongAfter ) == null );

		Thread.sleep( 15 );

		whileLocked = cache.nextTimestamp();

		assertTrue( !ccs.put( "foo", "foo", whileLocked, null, null, false ) );

		Thread.sleep( 15 );

		ccs.release( "foo", lock2 );

		Thread.sleep( 15 );

		long betweenReleases = cache.nextTimestamp();

		assertTrue( !ccs.put( "foo", "bar", betweenReleases, null, null, false ) );
		assertTrue( ccs.get( "foo", betweenReleases ) == null );

		Thread.sleep( 15 );

		ccs.release( "foo", lock1 );

		assertTrue( !ccs.put( "foo", "bar", whileLocked, null, null, false ) );

		Thread.sleep( 15 );

		longAfter = cache.nextTimestamp();

		assertTrue( ccs.put( "foo", "baz", longAfter, null, null, false ) );
		assertTrue( ccs.get( "foo", whileLocked ) == null );

		Thread.sleep( 15 );

		longLongAfter = cache.nextTimestamp();

		assertTrue( ccs.get( "foo", longLongAfter ).equals( "baz" ) );

	}

}
