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

import java.io.Serializable;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.internal.util.compare.EqualsHelper;

/**
 * Abstract adapter for Java type descriptors.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTypeDescriptor<T> implements JavaTypeDescriptor<T>, Serializable {
	private final Class<T> type;
	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> comparator;

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 *
	 * @see #AbstractTypeDescriptor(Class, MutabilityPlan)
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractTypeDescriptor(Class<T> type) {
		this( type, (MutabilityPlan<T>) ImmutableMutabilityPlan.INSTANCE );
	}

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 * @param mutabilityPlan The plan for handling mutability aspects of the java type.
	 */
	@SuppressWarnings({ "unchecked" })
	protected AbstractTypeDescriptor(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		this.type = type;
		this.mutabilityPlan = mutabilityPlan;
		this.comparator = Comparable.class.isAssignableFrom( type )
				? (Comparator<T>) ComparableComparator.INSTANCE
				: null;

		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( this );
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public Class<T> getJavaTypeClass() {
		return type;
	}

	@Override
	public int extractHashCode(T value) {
		return value.hashCode();
	}

	@Override
	public boolean areEqual(T one, T another) {
		return EqualsHelper.equals( one, another );
	}

	@Override
	public Comparator<T> getComparator() {
		return comparator;
	}

	@Override
	public String extractLoggableRepresentation(T value) {
		return (value == null) ? "null" : value.toString();
	}

	protected HibernateException unknownUnwrap(Class conversionType) {
		throw new HibernateException(
				"Unknown unwrap conversion requested: " + type.getName() + " to " + conversionType.getName()
		);
	}

	protected HibernateException unknownWrap(Class conversionType) {
		throw new HibernateException(
				"Unknown wrap conversion requested: " + conversionType.getName() + " to " + type.getName()
		);
	}
}
