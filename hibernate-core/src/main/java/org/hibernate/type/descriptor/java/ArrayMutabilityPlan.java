/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;
import org.hibernate.internal.build.AllowReflection;

import java.lang.reflect.Array;

/**
 * A mutability plan for arrays.  Specifically arrays of immutable element type; since the elements themselves
 * are immutable, a shallow copy is enough.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@link ImmutableObjectArrayMutabilityPlan#get()} for object arrays,
 * or implement a dedicated mutability plan for primitive arrays
 * (see for example {@link ShortPrimitiveArrayJavaType}'s mutability plan).
 */
@Deprecated
public class ArrayMutabilityPlan<T> extends MutableMutabilityPlan<T> {
	public static final ArrayMutabilityPlan INSTANCE = new ArrayMutabilityPlan();

	@SuppressWarnings({ "unchecked", "SuspiciousSystemArraycopy" })
	@AllowReflection
	public T deepCopyNotNull(T value) {
		if ( ! value.getClass().isArray() ) {
			// ugh!  cannot find a way to properly define the type signature here
			throw new IllegalArgumentException( "Value was not an array [" + value.getClass().getName() + "]" );
		}
		final int length = Array.getLength( value );
		T copy = (T) Array.newInstance( value.getClass().getComponentType(), length );
		System.arraycopy( value, 0, copy, 0, length );
		return copy;
	}
}
