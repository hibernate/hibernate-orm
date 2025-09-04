/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class SparseDoubleVectorUnitTest {

	@Test
	public void testEmpty() {
		final SparseDoubleVector doubles = new SparseDoubleVector( 3 );
		doubles.set( 1, (double) 3 );
		assertArrayEquals( new Object[] {(double) 0, (double) 3, (double) 0}, doubles.toArray() );
	}

	@Test
	public void testInsertBefore() {
		final SparseDoubleVector doubles = new SparseDoubleVector( 3, new int[] {1}, new double[] {3} );
		doubles.set( 0, (double) 2 );
		assertArrayEquals( new Object[] {(double) 2, (double) 3, (double) 0}, doubles.toArray() );
	}

	@Test
	public void testInsertAfter() {
		final SparseDoubleVector doubles = new SparseDoubleVector( 3, new int[] {1}, new double[] {3} );
		doubles.set( 2, (double) 2 );
		assertArrayEquals( new Object[] {(double) 0, (double) 3, (double) 2}, doubles.toArray() );
	}

	@Test
	public void testReplace() {
		final SparseDoubleVector doubles = new SparseDoubleVector( 3, new int[] {0, 1, 2}, new double[] {3, 3, 3} );
		doubles.set( 2, (double) 2 );
		assertArrayEquals( new Object[] {(double) 3, (double) 3, (double) 2}, doubles.toArray() );
	}

	@Test
	public void testFromDenseVector() {
		final SparseDoubleVector doubles = new SparseDoubleVector( new double[] {0, 3, 0} );
		assertArrayEquals( new Object[] {(double) 0, (double) 3, (double) 0}, doubles.toArray() );
	}

	@Test
	public void testFromDenseVectorList() {
		final SparseDoubleVector doubles = new SparseDoubleVector( List.of( (double) 0, (double) 3, (double) 0 ) );
		assertArrayEquals( new Object[] {(double) 0, (double) 3, (double) 0}, doubles.toArray() );
	}
}
