/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.Arrays;

import org.hibernate.SharedSessionContract;

/**
 * Mutability plan for component based arrays.
 *
 * @author Christian Beikov
 */
public class ComponentArrayMutabilityPlan implements MutabilityPlan<Object[]> {

	private final JavaType<Object>[] components;

	public ComponentArrayMutabilityPlan(JavaType<Object>[] components) {
		this.components = components;
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Object disassemble(Object[] value, SharedSessionContract session) {
		return deepCopy( value );
	}

	@Override
	public Object[] assemble(Object cached, SharedSessionContract session) {
		return deepCopy( (Object[]) cached );
	}

	@Override
	public final Object[] deepCopy(Object[] value) {
		if ( value == null ) {
			return null;
		}
		if ( value.length != components.length ) {
			throw new IllegalArgumentException(
					"Value does not have the expected size " + components.length + ": " + Arrays.toString( value )
			);
		}
		final Object[] copy = new Object[value.length];
		for ( int i = 0; i < components.length; i++ ) {
			copy[i] = components[i].getMutabilityPlan().deepCopy( value[i] );
		}
		return copy;
	}
}
