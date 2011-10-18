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
package org.hibernate.test.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Test;

import org.hibernate.internal.util.collections.JoinedIterable;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class JoinedIterableTest extends BaseUnitTestCase {
	@Test
	public void testNullIterables() {
		try {
			new JoinedIterable<String>( null );
			fail();
		}
		catch (NullPointerException ex) {
			// expected
		}
	}

	@Test
	public void testSingleEmptyIterable() {
		Set<String> emptyList = new HashSet<String>();
		List<Iterable<String>> iterableSets = new ArrayList<Iterable<String>>(  );
		iterableSets.add( emptyList );
		Iterable<String> iterable = new JoinedIterable<String>( iterableSets );
		assertFalse( iterable.iterator().hasNext() );
		try {
			iterable.iterator().next();
			fail( "Should have thrown NoSuchElementException because the underlying collection is empty.");
		}
		catch ( NoSuchElementException ex ) {
			// expected
		}
		try {
			iterable.iterator().remove();
			fail( "Should have thrown IllegalStateException because the underlying collection is empty." );
		}
		catch ( IllegalStateException ex ) {
			// expected
		}
		for ( String s : iterable ) {
			fail( "Should not have entered loop because underlying collection is empty");
		}
	}

	@Test
	public void testSingleIterableOfSingletonCollection() {
		final String str = "a string";
		Set<String> singleTonSet = new HashSet<String>( 1 );
		singleTonSet.add( str );
		List<Iterable<String>> iterableSets = new ArrayList<Iterable<String>>(  );
		iterableSets.add( singleTonSet );
		Iterable<String> iterable = new JoinedIterable<String>( iterableSets );
		assertTrue( iterable.iterator().hasNext() );
		assertSame( str, iterable.iterator().next() );
		assertFalse( iterable.iterator().hasNext() );
		try {
			iterable.iterator().next();
			fail( "Should have thrown NoSuchElementException because the underlying collection is empty.");
		}
		catch ( NoSuchElementException ex ) {
			// expected
		}
		for ( String s : iterable ) {
			fail( "should not have entered loop because underlying iterator should have been exhausted." );
		}
		assertEquals( 1, singleTonSet.size() );
		iterable = new JoinedIterable<String>( iterableSets );
		for ( String s : iterable ) {
			assertSame( str, s );
			iterable.iterator().remove();
		}
		assertTrue( singleTonSet.isEmpty() );
	}

	@Test
	public void testJoinedIterables() {
		List<Iterable<Integer>> listOfIterables = new ArrayList<Iterable<Integer>>(  );

		List<Integer> twoElementList = Arrays.asList( 0, 1 );
		listOfIterables.add( twoElementList );

		List<Integer> emptyList = new ArrayList<Integer>(  );
		listOfIterables.add( emptyList );

		List<Integer> oneElementList = Arrays.asList( 2 );
		listOfIterables.add( oneElementList );

		List<Integer> threeElementList = Arrays.asList( 3, 4, 5 );
		listOfIterables.add( threeElementList );

		JoinedIterable<Integer> joinedIterable = new JoinedIterable<Integer>( listOfIterables );

		int i = 0;
		for ( Integer val : joinedIterable ) {
			assertEquals( Integer.valueOf( i ), val );
			i++;
		}
	}
}
