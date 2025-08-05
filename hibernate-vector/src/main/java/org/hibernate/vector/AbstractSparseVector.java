/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.hibernate.internal.util.collections.ArrayHelper;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Base class for sparse vectors.
 *
 * @since 7.1
 */
public abstract class AbstractSparseVector<E> extends AbstractList<E> {

	protected static final int[] EMPTY_INT_ARRAY = new int[0];

	protected interface ElementParser<V> {
		V parse(String string, int start, int end);
	}
	protected record ParsedVector<V>(int size, int[] indices, List<V> elements) {
	}

	protected static <V> ParsedVector<V> parseSparseVector(String string, ElementParser<V> parser) {
		if ( string == null || !string.startsWith( "[" ) || !string.endsWith( "]" ) ) {
			throw invalidVector( string );
		}
		final int lengthEndIndex = string.indexOf( ',', 2 );
		if ( lengthEndIndex == -1 ) {
			throw invalidVector( string );
		}
		final int indicesStartIndex = lengthEndIndex + 1;
		if ( string.charAt( indicesStartIndex ) != '[' ) {
			throw invalidVector( string );
		}
		final int indicesEndIndex = string.indexOf( ']', indicesStartIndex + 1 );
		if ( indicesEndIndex == -1 ) {
			throw invalidVector( string );
		}
		final int commaIndex = indicesEndIndex + 1;
		if ( string.charAt( commaIndex ) != ',' ) {
			throw invalidVector( string );
		}
		final int elementsStartIndex = commaIndex + 1;
		if ( string.charAt( elementsStartIndex ) != '[' ) {
			throw invalidVector( string );
		}
		final int elementsEndIndex = string.indexOf( ']', elementsStartIndex + 1 );
		if ( elementsEndIndex == -1 ) {
			throw invalidVector( string );
		}
		if ( elementsEndIndex != string.length() - 2 ) {
			throw invalidVector( string );
		}
		final int size = Integer.parseInt( string, 1, lengthEndIndex, 10 );
		int start = indicesStartIndex + 1;
		final List<Integer> indicesList = new ArrayList<>();
		if ( start < indicesEndIndex ) {
			for ( int i = start; i < indicesEndIndex; i++ ) {
				if ( string.charAt( i ) == ',' ) {
					indicesList.add( Integer.parseInt( string, start, i, 10 ) );
					start = i + 1;
				}
			}
			indicesList.add( Integer.parseInt( string, start, indicesEndIndex, 10 ) );
		}
		final int[] indices = ArrayHelper.toIntArray( indicesList );
		final List<V> elements = new ArrayList<>( indices.length );
		start = elementsStartIndex + 1;
		if ( start < elementsEndIndex ) {
			for ( int i = start; i < elementsEndIndex; i++ ) {
				if ( string.charAt( i ) == ',' ) {
					elements.add( parser.parse( string, start, i ) );
					start = i + 1;
				}
			}
			elements.add( parser.parse( string, start, elementsEndIndex ) );
		}
		return new ParsedVector<>( size, indices, elements );
	}

	private static IllegalArgumentException invalidVector(String string) {
		return new IllegalArgumentException( "Invalid sparse vector string: " + string );
	}

	protected static int[] validateIndices(int[] indices, int dataLength, int size) {
		if ( indices == null ) {
			throw new IllegalArgumentException( "indices cannot be null" );
		}
		if ( indices.length != dataLength ) {
			throw new IllegalArgumentException( "indices length does not match data length" );
		}
		int previousIndex = -1;
		for ( int i = 0; i < indices.length; i++ ) {
			if ( indices[i] < 0 ) {
				throw new IllegalArgumentException( "indices[" + i + "] < 0" );
			}
			else if ( indices[i] < previousIndex ) {
				throw new IllegalArgumentException( "Indices array is not sorted ascendingly." );
			}
			previousIndex = indices[i];
		}
		if ( previousIndex >= size ) {
			throw new IllegalArgumentException( "Indices array contains index " + previousIndex + " that is greater than or equal to size: " + size );
		}
		return indices;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException( "Cannot remove from sparse vector" );
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException( "Cannot remove from sparse vector" );
	}

	@Override
	public boolean add(E aByte) {
		throw new UnsupportedOperationException( "Cannot add to sparse vector" );
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException( "Cannot add to sparse vector" );
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException( "Cannot add to sparse vector" );
	}
}
