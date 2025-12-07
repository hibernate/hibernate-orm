/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;

/**
 * Mutability plan for mutable objects
 *
 * @author Steve Ebersole
 */
public abstract class MutableMutabilityPlan<T> implements MutabilityPlan<T> {

	public static <T> MutableMutabilityPlan<T> instance() {
		//noinspection unchecked
		return INSTANCE;
	}

	public static final MutableMutabilityPlan INSTANCE = new MutableMutabilityPlan<>() {
		@Override
		protected Object deepCopyNotNull(Object value) {
			return value;
		}
	};

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(T value, SharedSessionContract session) {
		return (Serializable) deepCopy( value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T assemble(Serializable cached, SharedSessionContract session) {
		return deepCopy( (T) cached );
	}

	@Override
	public final T deepCopy(T value) {
		return value == null ? null : deepCopyNotNull( value );
	}

	protected abstract T deepCopyNotNull(T value);
}
