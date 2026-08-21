/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class SparseByteVectorUnitTest {

	@Test
	public void testEmpty() {
		final SparseByteVector bytes = new SparseByteVector( 3 );
		bytes.set( 1, (byte) 3 );
		assertArrayEquals( new Object[] {(byte) 0, (byte) 3, (byte) 0}, bytes.toArray() );
	}

	@Test
	public void testInsertBefore() {
		final SparseByteVector bytes = new SparseByteVector( 3, new int[] {1}, new byte[] {3} );
		bytes.set( 0, (byte) 2 );
		assertArrayEquals( new Object[] {(byte) 2, (byte) 3, (byte) 0}, bytes.toArray() );
	}

	@Test
	public void testInsertAfter() {
		final SparseByteVector bytes = new SparseByteVector( 3, new int[] {1}, new byte[] {3} );
		bytes.set( 2, (byte) 2 );
		assertArrayEquals( new Object[] {(byte) 0, (byte) 3, (byte) 2}, bytes.toArray() );
	}

	@Test
	public void testReplace() {
		final SparseByteVector bytes = new SparseByteVector( 3, new int[] {0, 1, 2}, new byte[] {3, 3, 3} );
		bytes.set( 2, (byte) 2 );
		assertArrayEquals( new Object[] {(byte) 3, (byte) 3, (byte) 2}, bytes.toArray() );
	}

	@Test
	public void testFromDenseVector() {
		final SparseByteVector bytes = new SparseByteVector( new byte[] {0, 3, 0} );
		assertArrayEquals( new Object[] {(byte) 0, (byte) 3, (byte) 0}, bytes.toArray() );
	}

	@Test
	public void testFromDenseVectorList() {
		final SparseByteVector bytes = new SparseByteVector( List.of( (byte) 0, (byte) 3, (byte) 0 ) );
		assertArrayEquals( new Object[] {(byte) 0, (byte) 3, (byte) 0}, bytes.toArray() );
	}
}
