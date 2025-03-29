/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

/**
 * A mutability plan for mutable arrays of immutable, non-primitive objects.
 * <p>
 * Since the elements themselves are immutable, the deep copy can be implemented with a shallow copy.
 *
 * @author Steve Ebersole
 */
public final class ImmutableObjectArrayMutabilityPlan<T> extends MutableMutabilityPlan<T[]> {
	@SuppressWarnings("rawtypes")
	private static final ImmutableObjectArrayMutabilityPlan INSTANCE = new ImmutableObjectArrayMutabilityPlan();

	@SuppressWarnings("unchecked") // Works for any T
	public static <T> ImmutableObjectArrayMutabilityPlan<T> get() {
		return (ImmutableObjectArrayMutabilityPlan<T>) INSTANCE;
	}

	public T[] deepCopyNotNull(T[] value) {
		return value.clone();
	}
}
