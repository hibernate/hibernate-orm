/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class SparseFloatVectorUnitTest {

	@Test
	public void testEmpty() {
		final SparseFloatVector floats = new SparseFloatVector( 3 );
		floats.set( 1, (float) 3 );
		assertArrayEquals( new Object[] {(float) 0, (float) 3, (float) 0}, floats.toArray() );
	}

	@Test
	public void testInsertBefore() {
		final SparseFloatVector floats = new SparseFloatVector( 3, new int[] {1}, new float[] {3} );
		floats.set( 0, (float) 2 );
		assertArrayEquals( new Object[] {(float) 2, (float) 3, (float) 0}, floats.toArray() );
	}

	@Test
	public void testInsertAfter() {
		final SparseFloatVector floats = new SparseFloatVector( 3, new int[] {1}, new float[] {3} );
		floats.set( 2, (float) 2 );
		assertArrayEquals( new Object[] {(float) 0, (float) 3, (float) 2}, floats.toArray() );
	}

	@Test
	public void testReplace() {
		final SparseFloatVector floats = new SparseFloatVector( 3, new int[] {0, 1, 2}, new float[] {3, 3, 3} );
		floats.set( 2, (float) 2 );
		assertArrayEquals( new Object[] {(float) 3, (float) 3, (float) 2}, floats.toArray() );
	}

	@Test
	public void testFromDenseVector() {
		final SparseFloatVector floats = new SparseFloatVector( new float[] {0, 3, 0} );
		assertArrayEquals( new Object[] {(float) 0, (float) 3, (float) 0}, floats.toArray() );
	}

	@Test
	public void testFromDenseVectorList() {
		final SparseFloatVector floats = new SparseFloatVector( List.of( (float) 0, (float) 3, (float) 0 ) );
		assertArrayEquals( new Object[] {(float) 0, (float) 3, (float) 0}, floats.toArray() );
	}
}
