/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

/**
 * Mutability plan for immutable objects
 *
 * @author Steve Ebersole
 */
public class ImmutableMutabilityPlan<T> implements MutabilityPlan<T> {
	public static final ImmutableMutabilityPlan INSTANCE = new ImmutableMutabilityPlan();

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public T deepCopy(T value) {
		return value;
	}

	@Override
	public Serializable disassemble(T value) {
		return (Serializable) value;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public T assemble(Serializable cached) {
		return (T) cached;
	}
}
