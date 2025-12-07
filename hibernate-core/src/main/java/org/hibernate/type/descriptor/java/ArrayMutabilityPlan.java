/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.internal.build.AllowReflection;

import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.newInstance;

/**
 * A mutability plan for arrays. Specifically arrays of immutable element type;
 * since the elements themselves are immutable, a shallow copy is enough.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@link ImmutableObjectArrayMutabilityPlan#get()} for object arrays,
 *             or implement a dedicated mutability plan for primitive arrays
 *             (see for example {@link ShortPrimitiveArrayJavaType}'s mutability plan).
 */
@Deprecated(forRemoval = true, since = "7.0")
public class ArrayMutabilityPlan<T> extends MutableMutabilityPlan<T> {
	public static final ArrayMutabilityPlan INSTANCE = new ArrayMutabilityPlan();

	@SuppressWarnings({ "unchecked", "SuspiciousSystemArraycopy" })
	@AllowReflection
	public T deepCopyNotNull(T value) {
		final var valueClass = value.getClass();
		if ( !valueClass.isArray() ) {
			// ugh!  cannot find a way to properly define the type signature here
			throw new IllegalArgumentException( "Value was not an array [" + valueClass.getName() + "]" );
		}
		final int length = getLength( value );
		final Object copy = newInstance( valueClass.getComponentType(), length );
		System.arraycopy( value, 0, copy, 0, length );
		return (T) copy;
	}
}
