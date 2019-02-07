/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.type.BasicType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicTypeMutabilityPlanAdapter<T> implements MutabilityPlan<T> {
	private final BasicType basicType;

	public BasicTypeMutabilityPlanAdapter(BasicType basicType) {
		this.basicType = basicType;
	}

	@Override
	public boolean isMutable() {
		return basicType.isMutable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T deepCopy(T value) {
		return (T) basicType.deepCopy( value );
	}

	@Override
	public Serializable disassemble(T value) {
		return basicType.disassemble( value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public T assemble(Serializable cached) {
		return (T) basicType.assemble( cached );
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

		return getReplacement( originalValue, targetValue );
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
				? getReplacement( originalValue, targetValue )
				: targetValue;
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected T getReplacement(T original, T target) {
		final JavaTypeDescriptor<T> javaTypeDescriptor = (JavaTypeDescriptor<T>) basicType.getJavaTypeDescriptor();
		if ( !isMutable() || ( target != null && javaTypeDescriptor.areEqual( original, target ) ) ) {
			return original;
		}
		else {
			return deepCopy( original );
		}
	}
}
