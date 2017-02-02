/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;
import java.lang.reflect.Array;

/**
 * A mutability plan for arrays.  Specifically arrays of immutable element type; since the elements themselves
 * are immutable, a shallow copy is enough.
 *
 * @author Steve Ebersole
 */
public class ArrayMutabilityPlan<T> extends MutableMutabilityPlan<T> {
	public static final ArrayMutabilityPlan INSTANCE = new ArrayMutabilityPlan();

	@SuppressWarnings({ "unchecked", "SuspiciousSystemArraycopy" })
	public T deepCopyNotNull(T value) {
		if ( ! value.getClass().isArray() ) {
			// ugh!  cannot find a way to properly define the type signature here to
			throw new IllegalArgumentException( "Value was not an array [" + value.getClass().getName() + "]" );
		}
		final int length = Array.getLength( value );
		T copy = (T) Array.newInstance( value.getClass().getComponentType(), length );
		System.arraycopy( value, 0, copy, 0, length );
		return copy;
	}
}
