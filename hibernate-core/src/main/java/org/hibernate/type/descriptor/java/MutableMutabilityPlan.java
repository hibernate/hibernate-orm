/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Base mutability plan for mutable values. Subclasses implement the non-null
/// deep-copy operation; this base handles nulls and cache assembly/disassembly.
///
/// @param <T> the planned Java value type
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class MutableMutabilityPlan<T> implements MutabilityPlan<T> {

	/// Constructor for provider subclasses.
	@SPI(IMPLEMENT)
	protected MutableMutabilityPlan() {
	}

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
