/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 *
 */
package org.hibernate.event.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.event.spi.EventSource;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 2011/10/20 Unit test for code added in MergeContext for performance improvement.
 *
 * @author Wim Ockerman @ CISCO
 */
public class MergeContextTest extends BaseCoreFunctionalTestCase {
	private EventSource session = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Simple.class };
	}

	@Before
	public void setUp() {
		session = (EventSource) openSession();
	}

	@After
	public void tearDown() {
		session.close();
		session = null;
	}

	@Test
    public void testMergeToManagedEntityFillFollowedByInvertMapping() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

        Object mergeEntity = new Simple( 1 );
        Object managedEntity = new Simple( 2 );
        
        cache.put(mergeEntity, managedEntity);

		checkCacheConsistency( cache, 1 );

		assertTrue( cache.containsKey( mergeEntity ) );
        assertFalse( cache.containsKey( managedEntity ) );
		assertTrue( cache.containsValue( managedEntity ) );

		assertTrue( cache.invertMap().containsKey( managedEntity ) );
        assertFalse( cache.invertMap().containsKey( mergeEntity ) );
		assertTrue( cache.invertMap().containsValue( mergeEntity ) );

		cache.clear();

		checkCacheConsistency( cache, 0 );

		assertFalse(cache.containsKey(mergeEntity));
        assertFalse(cache.invertMap().containsKey(managedEntity));
	}

	@Test
    public void testMergeToManagedEntityFillFollowedByInvert() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

        Object mergeEntity = new Simple( 1 );
        Object managedEntity = new Simple( 2 );
        
        cache.put(mergeEntity, managedEntity);

		checkCacheConsistency( cache, 1 );

		assertTrue(cache.containsKey(mergeEntity));
        assertFalse( cache.containsKey( managedEntity ) );
        
        assertTrue( cache.invertMap().containsKey( managedEntity ) );
        assertFalse( cache.invertMap().containsKey( mergeEntity ) );
    }

	@Test
    public void testMergeToManagedEntityFillFollowedByInvertUsingPutAll() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

        Map<Object,Object> input = new HashMap<Object,Object>();
        Object mergeEntity1 = new Simple( 1 );
		//
        Object managedEntity1 = 1;
        input.put(mergeEntity1, managedEntity1);
        Object mergeEntity2 = new Simple( 3 );
        Object managedEntity2 = 2;
        input.put(mergeEntity2, managedEntity2);
        cache.putAll(input);

		checkCacheConsistency( cache, 2 );

		assertTrue(cache.containsKey(mergeEntity1));
        assertFalse(cache.containsKey(managedEntity1));
        assertTrue(cache.containsKey(mergeEntity2));
        assertFalse(cache.containsKey(managedEntity2));

        assertTrue(cache.invertMap().containsKey(managedEntity1));
        assertFalse(cache.invertMap().containsKey(mergeEntity1));

        assertTrue(cache.invertMap().containsKey(managedEntity2));
        assertFalse(cache.invertMap().containsKey(mergeEntity2));
    }

	@Test
    public void testMergeToManagedEntityFillFollowedByInvertUsingPutWithSetOperatedOnArg() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

        Object mergeEntity = new Simple( 1 );
        Object managedEntity = new Simple( 2 );
        
        cache.put(mergeEntity, managedEntity, true);

		checkCacheConsistency( cache, 1 );

		assertTrue(cache.containsKey(mergeEntity));
        assertFalse( cache.containsKey( managedEntity ) );

        assertTrue( cache.invertMap().containsKey( managedEntity ) );
        assertFalse( cache.invertMap().containsKey( mergeEntity ) );
        
        cache.clear();

		checkCacheConsistency( cache, 0 );

		cache.put(mergeEntity, managedEntity, false);
		assertFalse( cache.isOperatedOn( mergeEntity ) );

		checkCacheConsistency( cache, 1 );

		assertTrue(cache.containsKey(mergeEntity));
        assertFalse(cache.containsKey(managedEntity));
    }

	@Test
	public void testMergeToManagedEntityFillFollowedByIterateEntrySet() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

		Object mergeEntity = new Simple( 1 );
		Object managedEntity = new Simple( 2 );

		cache.put( mergeEntity, managedEntity, true );

		checkCacheConsistency( cache, 1 );

		Iterator it = cache.entrySet().iterator();
		assertTrue( it.hasNext() );
		Map.Entry entry = ( Map.Entry ) it.next();
		assertSame( mergeEntity, entry.getKey() );
		assertSame( managedEntity, entry.getValue() );
		assertFalse( it.hasNext() );

	}

	@Test
	public void testMergeToManagedEntityFillFollowedByModifyEntrySet() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

		Object mergeEntity = new Simple( 1 );
		Object managedEntity = new Simple( 2 );

		cache.put( mergeEntity, managedEntity, true );

		Iterator it = cache.entrySet().iterator();
		try {
			it.remove();
			fail( "should have thrown UnsupportedOperationException" );
		}
		catch ( UnsupportedOperationException ex ) {
			// expected
		}

		Map.Entry entry = (Map.Entry) cache.entrySet().iterator().next();
		try {
			cache.entrySet().remove( entry );
			fail( "should have thrown UnsupportedOperationException" );
		}
		catch ( UnsupportedOperationException ex ) {
			// expected
		}

		Map.Entry anotherEntry = new Map.Entry() {
			private Object key = new Simple( 3 );
			private Object value = 4;
			@Override
			public Object getKey() {
				return key;
			}

			@Override
			public Object getValue() {
				return value;
			}

			@Override
			public Object setValue(Object value) {
				Object oldValue = this.value;
				this.value = value;
				return oldValue;
			}
		};
		try {
			cache.entrySet().add( anotherEntry );
			fail( "should have thrown UnsupportedOperationException" );
		}
		catch ( UnsupportedOperationException ex ) {
			// expected
		}

	}

	@Test
	public void testMergeToManagedEntityFillFollowedByModifyKeys() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

		Object mergeEntity = new Simple( 1 );
		Object managedEntity = new Simple( 2 );

		cache.put( mergeEntity, managedEntity, true );

		Iterator it = cache.keySet().iterator();
		try {
			it.remove();
			fail( "should have thrown UnsupportedOperationException" );
		}
		catch ( UnsupportedOperationException ex ) {
			// expected
		}

		try {
			cache.keySet().remove( mergeEntity );
			fail( "should have thrown UnsupportedOperationException" );
		}
		catch ( UnsupportedOperationException ex ) {
			// expected
		}

		Object newmanagedEntity = new Simple( 3 );
		try {
			cache.keySet().add( newmanagedEntity );
			fail( "should have thrown UnsupportedOperationException" );
		}
		catch ( UnsupportedOperationException ex ) {
			// expected
		}
	}

	@Test
	public void testMergeToManagedEntityFillFollowedByModifyValues() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

		Object mergeEntity = new Simple( 1 );
		Object managedEntity = new Simple( 2 );

		cache.put( mergeEntity, managedEntity, true );

		Iterator it = cache.values().iterator();
		try {
			it.remove();
			fail( "should have thrown UnsupportedOperationException" );
		}
		catch ( UnsupportedOperationException ex ) {
			// expected
		}

		try {
			cache.values().remove( managedEntity );
			fail( "should have thrown UnsupportedOperationException" );
		}
		catch ( UnsupportedOperationException ex ) {
			// expected
		}

		Object newmanagedEntity = new Simple( 3 );
		try {
			cache.values().add( newmanagedEntity );
			fail( "should have thrown UnsupportedOperationException" );
		}
		catch ( UnsupportedOperationException ex ) {
			// expected
		}
	}

	@Test
	public void testMergeToManagedEntityFillFollowedByModifyKeyOfEntrySetElement() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

		Simple mergeEntity = new Simple( 1 );
		Simple managedEntity = new Simple( 0 );
		cache.put(mergeEntity, managedEntity, true);

		Map.Entry entry = (Map.Entry) cache.entrySet().iterator().next();
		( ( Simple ) entry.getKey() ).setValue( 2 );
		assertEquals( 2, mergeEntity.getValue() );

		checkCacheConsistency( cache, 1 );

		entry = (Map.Entry) cache.entrySet().iterator().next();
		assertSame( mergeEntity, entry.getKey() );
		assertSame( managedEntity, entry.getValue() );
	}

	@Test
	public void testMergeToManagedEntityFillFollowedByModifyValueOfEntrySetElement() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

		Simple mergeEntity = new Simple( 1 );
		Simple managedEntity = new Simple( 0 );
		cache.put(mergeEntity, managedEntity, true);

		Map.Entry entry = (Map.Entry) cache.entrySet().iterator().next();
		( ( Simple ) entry.getValue() ).setValue( 2 );
		assertEquals( 2, managedEntity.getValue() );

		checkCacheConsistency( cache, 1 );

		entry = (Map.Entry) cache.entrySet().iterator().next();
		assertSame( mergeEntity, entry.getKey() );
		assertSame( managedEntity, entry.getValue() );
	}

	@Test
	public void testReplaceManagedEntity() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

		Simple mergeEntity = new Simple( 1 );
		Simple managedEntity = new Simple( 0 );
		cache.put(mergeEntity, managedEntity);

		Simple managedEntityNew = new Simple( 0 );
		try {
			cache.put( mergeEntity, managedEntityNew );
		}
		catch( IllegalArgumentException ex) {
			// expected; cannot replace the managed entity result for a particular merge entity.
		}
	}

	@Test
	public void testManagedEntityAssociatedWithNewAndExistingMergeEntities() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

		session.getTransaction().begin();
		Simple mergeEntity = new Simple( 1 );
		Simple managedEntity = new Simple( 0 );
		cache.put(mergeEntity, managedEntity);
		cache.put( new Simple( 1 ), managedEntity );
	}

	@Test
	public void testManagedAssociatedWith2ExistingMergeEntities() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );

		session.getTransaction().begin();
		Simple mergeEntity1 = new Simple( 1 );
		session.persist( mergeEntity1 );
		Simple managedEntity1 = new Simple( 1 );
		cache.put( mergeEntity1, managedEntity1 );
		Simple managedEntity2 = new Simple( 2 );

		try {
			cache.put( mergeEntity1, managedEntity2 );
			fail( "should have thrown IllegalArgumentException");
		}
		catch( IllegalArgumentException ex ) {
			// expected; cannot change managed entity associated with a merge entity
		}
		finally {
			session.getTransaction().rollback();
		}
	}

	@Test
	public void testRemoveNonExistingEntity() {
		MergeContext cache = new MergeContext( session, new DoNothingEntityCopyObserver() );
		try {
			cache.remove( new Simple( 1 ) );
		}
		catch (UnsupportedOperationException ex) {
			// expected; remove is not supported.
		}
	}

	private void checkCacheConsistency(MergeContext cache, int expectedSize) {
		Set entrySet = cache.entrySet();
		Set cacheKeys = cache.keySet();
		Collection cacheValues = cache.values();
		Map invertedMap = cache.invertMap();

		assertEquals( expectedSize, entrySet.size() );
		assertEquals( expectedSize, cache.size() );
		assertEquals( expectedSize, cacheKeys.size() );
		assertEquals( expectedSize, cacheValues.size() );
		assertEquals( expectedSize, invertedMap.size() );

		for ( Object entry : cache.entrySet() ) {
			Map.Entry mapEntry = ( Map.Entry ) entry;
			assertSame( cache.get( mapEntry.getKey() ), mapEntry.getValue() );
			assertTrue( cacheKeys.contains( mapEntry.getKey() ) );
			assertTrue( cacheValues.contains( mapEntry.getValue() ) );
			assertSame( mapEntry.getKey(), invertedMap.get( mapEntry.getValue() ) );
		}
	}

	@Entity
	private static class Simple {
		@Id
		private int value;

		public Simple(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "Simple{" +
					"value=" + value +
					'}';
		}
	}

	private class DoNothingEntityCopyObserver implements EntityCopyObserver {

		@Override
		public void entityCopyDetected(Object managedEntity, Object mergeEntity1, Object mergeEntity2, EventSource session) {

		}

		@Override
		public void topLevelMergeComplete(EventSource session) {

		}

		@Override
		public void clear() {

		}
	}

	private class ExceptionThrowingEntityCopyObserver implements EntityCopyObserver {

		@Override
		public void entityCopyDetected(Object managedEntity, Object mergeEntity1, Object mergeEntity2, EventSource session) {
			throw new IllegalStateException( "Entity copies not allowed." );
		}

		@Override
		public void topLevelMergeComplete(EventSource session) {
		}

		@Override
		public void clear() {

		}
	}
}
