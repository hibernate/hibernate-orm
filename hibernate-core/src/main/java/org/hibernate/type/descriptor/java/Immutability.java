/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Mutability;

/**
 * Object-typed form of {@link ImmutableMutabilityPlan} for easier use
 * with {@link Mutability} for users
 *
 * @see org.hibernate.annotations.Immutable
 *
 * @author Steve Ebersole
 */
public class Immutability implements MutabilityPlan<Object> {
	/**
	 * Singleton access
	 *
	 * @deprecated in favor of {@link #instance()}
	 */
	@Deprecated( forRemoval = true )
	public static final Immutability INSTANCE = new Immutability();

	public static <X> MutabilityPlan<X> instance() {
		//noinspection unchecked
		return (MutabilityPlan<X>) INSTANCE;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object deepCopy(Object value) {
		return value;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContract session) {
		return (Serializable) value;
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContract session) {
		return cached;
	}
}
