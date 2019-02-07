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
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.MutabilityPlan;

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
	@SuppressWarnings("unchecked")
	public T replace(
			T originalValue,
			T targetValue,
			Object owner,
			Map copyCache,
			SessionImplementor session) {
		if ( originalValue == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			return targetValue;
		}
		else if ( !isMutable() ||
				( targetValue == LazyPropertyInitializer.UNFETCHED_PROPERTY &&
						basicType.getJavaTypeDescriptor().areEqual( originalValue, targetValue ) ) ) {
			return originalValue;
		}
		else {
			return deepCopy( originalValue );
		}
	}
}
