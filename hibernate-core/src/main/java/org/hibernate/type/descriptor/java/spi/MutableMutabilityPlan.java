/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.MutabilityPlan;

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

	@Override
	public T replace(
			Navigable<T> navigable,
			T originalValue,
			T targetValue,
			Object owner,
			Map copyCache,
			SessionImplementor session) {
		if ( originalValue == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			return targetValue;
		}

		return getReplacement( navigable, originalValue, targetValue );
	}

	@Override
	public T replace(
			Navigable<T> navigable,
			T originalValue,
			T targetValue,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection,
			SessionImplementor session) {
		return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection
				? getReplacement( navigable, originalValue, targetValue )
				: targetValue;
	}

	@SuppressWarnings("WeakerAccess")
	protected T getReplacement(Navigable<T> navigable, T original, T target) {
		final JavaTypeDescriptor<T> javaTypeDescriptor = navigable.getJavaTypeDescriptor();
		if ( !isMutable() || ( target != null && javaTypeDescriptor.areEqual( original, target ) ) ) {
			return original;
		}
		else {
			return deepCopy( original );
		}
	}

	protected abstract T deepCopyNotNull(T value);
}
