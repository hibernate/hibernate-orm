/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.compare.ComparableComparator;

/**
 * Abstract adapter for Java type descriptors.
 *
 * @apiNote This abstract descriptor implements BasicJavaDescriptor
 * because we currently only categorize "basic" JavaTypeDescriptors,
 * as in the {@link javax.persistence.metamodel.Type.PersistenceType#BASIC}
 * sense
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTypeDescriptor<T> implements BasicJavaDescriptor<T>, Serializable {
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
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	public Class<T> getJavaType() {
		return type;
	}

	/**
	 * @deprecated Use {@link #getJavaType()} instead
	 */
	@Override
	@Deprecated
	public Class<T> getJavaTypeClass() {
		return getJavaType();
	}

	@Override
	public int extractHashCode(T value) {
		return value.hashCode();
	}

	@Override
	public boolean areEqual(T one, T another) {
		return Objects.equals( one, another );
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
