/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.java;
import java.lang.reflect.Array;

/**
 * A mutability plan for arrays.  Specifically arrays of immutable element type; since the elements themselves
 * are immutable, a shallow copy is enough.
 *
 * @author Steve Ebersole
 */
public class ArrayMutabilityPlan<T> extends MutableMutabilityPlan<T> {
	public static final ArrayMutabilityPlan INSTANCE = new ArrayMutabilityPlan();

	@SuppressWarnings({ "unchecked", "SuspiciousSystemArraycopy" })
	public T deepCopyNotNull(T value) {
		if ( ! value.getClass().isArray() ) {
			// ugh!  cannot find a way to properly define the type signature here to
			throw new IllegalArgumentException( "Value was not an array [" + value.getClass().getName() + "]" );
		}
		final int length = Array.getLength( value );
		T copy = (T) Array.newInstance( value.getClass().getComponentType(), length );
		System.arraycopy( value, 0, copy, 0, length );
		return copy;
	}
}
