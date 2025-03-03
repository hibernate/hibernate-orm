/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;

/**
 * Mutability plan for immutable objects
 *
 * @apiNote For use with {@link org.hibernate.annotations.Mutability},
 * users should instead use {@link Immutability} as the type parameterization
 * here does not work with the parameterization defined on
 * {@link org.hibernate.annotations.Mutability#value}
 *
 * @author Steve Ebersole
 */
public class ImmutableMutabilityPlan<T> implements MutabilityPlan<T> {

	/**
	 * Singleton access
	 *
	 * @deprecated in favor of {@link #instance()}
	 */
	@SuppressWarnings( "rawtypes" )
	@Deprecated( forRemoval = true )
	public static final ImmutableMutabilityPlan INSTANCE = new ImmutableMutabilityPlan();

	public static <X> ImmutableMutabilityPlan<X> instance() {
		//noinspection unchecked
		return INSTANCE;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public T deepCopy(T value) {
		return value;
	}

	@Override
	public Serializable disassemble(T value, SharedSessionContract session) {
		return (Serializable) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T assemble(Serializable cached, SharedSessionContract session) {
		return (T) cached;
	}
}
