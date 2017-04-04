/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

/**
 * Mutability plan for mutable objects
 *
 * @author Steve Ebersole
 */
public abstract class MutableMutabilityPlan<T> implements MutabilityPlan<T> {
	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(T value) {
		return (Serializable) deepCopy( value );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public T assemble(Serializable cached) {
		return deepCopy( (T) cached );
	}

	@Override
	public final T deepCopy(T value) {
		return value == null ? null : deepCopyNotNull( value );
	}

	protected abstract T deepCopyNotNull(T value);
}
