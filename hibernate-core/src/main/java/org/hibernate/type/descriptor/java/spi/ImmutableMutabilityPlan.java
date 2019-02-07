/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.descriptor.java.MutabilityPlan;

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

	@Override
	public T replace(
			T originalValue,
			T targetValue,
			Object owner,
			Map copyCache,
			SessionImplementor session) {
		if ( originalValue == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			// todo (6.0) - Is this scenario possible?
			throw new NotYetImplementedFor6Exception( getClass() );
//			return targetValue;
		}
		else {
			return originalValue;
		}
	}
}
