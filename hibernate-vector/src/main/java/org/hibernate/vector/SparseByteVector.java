/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import java.util.Arrays;
import java.util.List;

/**
 * {@link java.util.List} implementation for a sparse byte vector.
 *
 * @since 7.1
 */
public class SparseByteVector extends AbstractSparseVector<Byte> {

	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	private final int size;
	private int[] indices = EMPTY_INT_ARRAY;
	private byte[] data = EMPTY_BYTE_ARRAY;

	public SparseByteVector(int size) {
		if ( size <= 0 ) {
			throw new IllegalArgumentException( "size must be greater than zero" );
		}
		this.size = size;
	}

	public SparseByteVector(List<Byte> list) {
		if ( list instanceof SparseByteVector sparseVector ) {
			size = sparseVector.size;
			indices = sparseVector.indices.clone();
			data = sparseVector.data.clone();
		}
		else {
			if ( list == null ) {
				throw new IllegalArgumentException( "list cannot be null" );
			}
			if ( list.isEmpty() ) {
				throw new IllegalArgumentException( "list cannot be empty" );
			}
			int size = 0;
			int[] indices = new int[list.size()];
			byte[] data = new byte[list.size()];
			for ( int i = 0; i < list.size(); i++ ) {
				final Byte b = list.get( i );
				if ( b != null && b != 0 ) {
					indices[size] = i;
					data[size] = b;
					size++;
				}
			}
			this.size = list.size();
			this.indices = Arrays.copyOf( indices, size );
			this.data = Arrays.copyOf( data, size );
		}
	}

	public SparseByteVector(byte[] denseVector) {
		if ( denseVector == null ) {
			throw new IllegalArgumentException( "denseVector cannot be null" );
		}
		if ( denseVector.length == 0 ) {
			throw new IllegalArgumentException( "denseVector cannot be empty" );
		}
		int size = 0;
		int[] indices = new int[denseVector.length];
		byte[] data = new byte[denseVector.length];
		for ( int i = 0; i < denseVector.length; i++ ) {
			final byte b = denseVector[i];
			if ( b != 0 ) {
				indices[size] = i;
				data[size] = b;
				size++;
			}
		}
		this.size = denseVector.length;
		this.indices = Arrays.copyOf( indices, size );
		this.data = Arrays.copyOf( data, size );
	}

	public SparseByteVector(int size, int[] indices, byte[] data) {
		this( validateData( data, size ), validateIndices( indices, data.length, size ), size );
	}

	private SparseByteVector(byte[] data, int[] indices, int size) {
		this.size = size;
		this.indices = indices;
		this.data = data;
	}

	public SparseByteVector(String string) {
		final ParsedVector<Byte> parsedVector =
				parseSparseVector( string, (s, start, end) -> Byte.parseByte( s.substring( start, end ) ) );
		this.size = parsedVector.size();
		this.indices = parsedVector.indices();
		this.data = toByteArray( parsedVector.elements() );
	}

	private static byte[] toByteArray(List<Byte> elements) {
		final byte[] result = new byte[elements.size()];
		for ( int i = 0; i < elements.size(); i++ ) {
			result[i] = elements.get(i);
		}
		return result;
	}

	private static byte[] validateData(byte[] data, int size) {
		if ( size == 0 ) {
			throw new IllegalArgumentException( "size cannot be 0" );
		}
		if ( data == null ) {
			throw new IllegalArgumentException( "data cannot be null" );
		}
		if ( size < data.length ) {
			throw new IllegalArgumentException( "size cannot be smaller than data size" );
		}
		for ( int i = 0; i < data.length; i++ ) {
			if ( data[i] == 0 ) {
				throw new IllegalArgumentException( "data[" + i + "] == 0" );
			}
		}
		return data;
	}

	@Override
	public SparseByteVector clone() {
		return new SparseByteVector( data.clone(), indices.clone(), size );
	}

	@Override
	public Byte get(int index) {
		final int foundIndex = Arrays.binarySearch( indices, index );
		return foundIndex < 0 ? 0 : data[foundIndex];
	}

	@Override
	public Byte set(int index, Byte element) {
		final int foundIndex = Arrays.binarySearch( indices, index );
		if ( foundIndex < 0 ) {
			if ( element != null && element != 0 ) {
				final int[] newIndices = new int[indices.length + 1];
				final byte[] newData = new byte[data.length + 1];
				final int insertionPoint = -foundIndex - 1;
				System.arraycopy( indices, 0, newIndices, 0, insertionPoint );
				System.arraycopy( data, 0, newData, 0, insertionPoint );
				newIndices[insertionPoint] = index;
				newData[insertionPoint] = element;
				System.arraycopy( indices, insertionPoint, newIndices, insertionPoint + 1, indices.length - insertionPoint );
				System.arraycopy( data, insertionPoint, newData, insertionPoint + 1, data.length - insertionPoint );
				this.indices = newIndices;
				this.data = newData;
			}
			return null;
		}
		else {
			final byte oldValue = data[foundIndex];
			if ( element != null && element != 0 ) {
				data[foundIndex] = element;
			}
			else {
				final int[] newIndices = new int[indices.length - 1];
				final byte[] newData = new byte[data.length - 1];
				System.arraycopy( indices, 0, newIndices, 0, foundIndex );
				System.arraycopy( data, 0, newData, 0, foundIndex );
				System.arraycopy( indices, foundIndex + 1, newIndices, foundIndex, indices.length - foundIndex - 1 );
				System.arraycopy( data, foundIndex + 1, newData, foundIndex, data.length - foundIndex - 1 );
				this.indices = newIndices;
				this.data = newData;
			}
			return oldValue;
		}
	}

	public byte[] toDenseVector() {
		final byte[] result = new byte[this.size];
		for ( int i = 0; i < indices.length; i++ ) {
			result[indices[i]] = data[i];
		}
		return result;
	}

	@Override
	public int[] indices() {
		return indices;
	}

	public byte[] bytes() {
		return data;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		return "[" + size +
			"," + Arrays.toString( indices ) +
			"," + Arrays.toString( data ) +
			"]";
	}
}
