/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	public static final MutableMutabilityPlan<Object> INSTANCE = new MutableMutabilityPlan<Object>() {
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
	public Object disassemble(T value, SharedSessionContract session) {
		return deepCopy( value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T assemble(Object cached, SharedSessionContract session) {
		return deepCopy( (T) cached );
	}

	@Override
	public final T deepCopy(T value) {
		return value == null ? null : deepCopyNotNull( value );
	}

	protected abstract T deepCopyNotNull(T value);
}
