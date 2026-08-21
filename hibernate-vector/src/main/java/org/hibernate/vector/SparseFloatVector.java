/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import java.util.Arrays;
import java.util.List;

/**
 * {@link List} implementation for a sparse byte vector.
 *
 * @since 7.2
 */
public class SparseFloatVector extends AbstractSparseVector<Float> {

	private static final float[] EMPTY_FLOAT_ARRAY = new float[0];

	private final int size;
	private int[] indices = EMPTY_INT_ARRAY;
	private float[] data = EMPTY_FLOAT_ARRAY;

	public SparseFloatVector(int size) {
		this.size = size;
	}

	public SparseFloatVector(List<Float> list) {
		if ( list instanceof SparseFloatVector sparseVector ) {
			size = sparseVector.size;
			indices = sparseVector.indices.clone();
			data = sparseVector.data.clone();
		}
		else {
			int size = 0;
			int[] indices = new int[list.size()];
			float[] data = new float[list.size()];
			for ( int i = 0; i < list.size(); i++ ) {
				final Float b = list.get( i );
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

	public SparseFloatVector(float[] denseVector) {
		int size = 0;
		int[] indices = new int[denseVector.length];
		float[] data = new float[denseVector.length];
		for ( int i = 0; i < denseVector.length; i++ ) {
			final float b = denseVector[i];
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

	public SparseFloatVector(int size, int[] indices, float[] data) {
		this( validateData( data ), validateIndices( indices, data.length, size ), size );
	}

	private SparseFloatVector(float[] data, int[] indices, int size) {
		this.size = size;
		this.indices = indices;
		this.data = data;
	}

	public SparseFloatVector(String string) {
		final ParsedVector<Float> parsedVector =
				parseSparseVector( string, (s, start, end) -> Float.parseFloat( s.substring( start, end ) ) );
		this.size = parsedVector.size();
		this.indices = parsedVector.indices();
		this.data = toFloatArray( parsedVector.elements() );
	}

	private static float[] toFloatArray(List<Float> elements) {
		final float[] result = new float[elements.size()];
		for ( int i = 0; i < elements.size(); i++ ) {
			result[i] = elements.get(i);
		}
		return result;
	}

	private static float[] validateData(float[] data) {
		if ( data == null ) {
			throw new IllegalArgumentException( "data cannot be null" );
		}
		for ( int i = 0; i < data.length; i++ ) {
			if ( data[i] == 0 ) {
				throw new IllegalArgumentException( "data[" + i + "] == 0" );
			}
		}
		return data;
	}

	@Override
	public SparseFloatVector clone() {
		return new SparseFloatVector( data.clone(), indices.clone(), size );
	}

	@Override
	public Float get(int index) {
		final int foundIndex = Arrays.binarySearch( indices, index );
		return foundIndex < 0 ? 0 : data[foundIndex];
	}

	@Override
	public Float set(int index, Float element) {
		final int foundIndex = Arrays.binarySearch( indices, index );
		if ( foundIndex < 0 ) {
			if ( element != null && element != 0 ) {
				final int[] newIndices = new int[indices.length + 1];
				final float[] newData = new float[data.length + 1];
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
			final float oldValue = data[foundIndex];
			if ( element != null && element != 0 ) {
				data[foundIndex] = element;
			}
			else {
				final int[] newIndices = new int[indices.length - 1];
				final float[] newData = new float[data.length - 1];
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

	public float[] toDenseVector() {
		final float[] result = new float[this.size];
		for ( int i = 0; i < indices.length; i++ ) {
			result[indices[i]] = data[i];
		}
		return result;
	}

	@Override
	public int[] indices() {
		return indices;
	}

	public float[] floats() {
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
