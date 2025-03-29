/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.tracker;

import org.hibernate.bytecode.enhance.internal.tracker.DirtyTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SimpleFieldTracker;
import org.hibernate.bytecode.enhance.internal.tracker.SortedFieldTracker;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ståle W. Pedersen
 */
public class DirtyTrackerTest {

	@Test
	public void testSimpleTracker() {
		DirtyTracker tracker = new SimpleFieldTracker();
		assertTrue( tracker.isEmpty() );
		assertEquals( 0, tracker.get().length );

		tracker.add( "foo" );
		assertFalse( tracker.isEmpty() );
		assertArrayEquals( tracker.get(), new String[]{"foo"} );

		tracker.clear();
		assertTrue( tracker.isEmpty() );
		assertEquals( 0, tracker.get().length );

		tracker.add( "foo" );
		tracker.add( "bar" );
		tracker.add( "another.bar" );
		tracker.add( "foo" );
		tracker.add( "another.foo" );
		tracker.add( "another.bar" );
		assertEquals( 4, tracker.get().length );

		tracker.suspend( true );
		tracker.add( "one more" );
		assertEquals( 4, tracker.get().length );
	}

	@Test
	public void testSortedTracker() {
		DirtyTracker tracker = new SortedFieldTracker();
		assertTrue( tracker.isEmpty() );
		assertEquals( 0, tracker.get().length );

		tracker.add( "foo" );
		assertFalse( tracker.isEmpty() );
		assertArrayEquals( tracker.get(), new String[]{"foo"} );

		tracker.clear();
		assertTrue( tracker.isEmpty() );
		assertEquals( 0, tracker.get().length );

		tracker.add( "foo" );
		tracker.add( "bar" );
		tracker.add( "another.bar" );
		tracker.add( "foo" );
		tracker.add( "another.foo" );
		tracker.add( "another.bar" );
		assertEquals( 4, tracker.get().length );

		// the algorithm for this implementation relies on the fact that the array is kept sorted, so let's check it really is
		assertTrue( isSorted( tracker.get() ) );

		tracker.suspend( true );
		tracker.add( "one more" );
		assertEquals( 4, tracker.get().length );
	}

	private boolean isSorted(String[] arr) {
		for ( int i = 1; i < arr.length; i++ ) {
			if ( arr[i - 1].compareTo( arr[i] ) > 0 ) {
				return false;
			}
		}
		return true;
	}
}
