/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.compare;

import java.lang.reflect.Array;
import java.util.Comparator;

/**
 * @author Gail Badner
 */
public final class ArrayComparator<X,Y extends Comparable> implements Comparator<X> {
	public static final Comparator<byte[]> PRIMITIVE_BYTE_ARRAY_COMPARATOR = new ArrayComparator<byte[],Byte>( Byte.class );
	public static final Comparator<Byte[]> BYTE_ARRAY_COMPARATOR = new ArrayComparator<Byte[],Byte>( Byte.class );
	public static final Comparator<char[]> PRIMITIVE_CHARACTER_ARRAY_COMPARATOR = new ArrayComparator<char[],Character>( Character.class );
	public static final Comparator<Character[]> CHARACTER_ARRAY_COMPARATOR = new ArrayComparator<Character[],Character>( Character.class );

	private final Class<Y> elementClass;

	private ArrayComparator(Class<Y> elementClass) {
		this.elementClass = elementClass;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public int compare(X o1, X o2) {
		final int lengthToCheck = Math.min( Array.getLength( o1 ), Array.getLength( o2 ) );
		for ( int i = 0 ; i < lengthToCheck ; i++ ) {
			int comparison = ComparableComparator.INSTANCE.compare(
					elementClass.cast( Array.get( o1, i ) ),
					elementClass.cast( Array.get( o2, i ) )
			);
			if ( comparison != 0 ) {
				return comparison;
			}
		}
		return Array.getLength( o1 ) - Array.getLength( o2 );
	}
}
