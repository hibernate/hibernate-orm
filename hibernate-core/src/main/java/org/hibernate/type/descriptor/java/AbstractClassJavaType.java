/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.compare.ComparableComparator;

/**
 * Abstract adapter for {@link JavaType Java type descriptors}.
 *
 * @apiNote This abstract descriptor implements {@link BasicJavaType}
 * because we currently only categorize basic {@link JavaType}s, where
 * "basic" is meant in the sense of the JPA specification, that is,
 * {@link jakarta.persistence.metamodel.Type.PersistenceType#BASIC}.
 *
 *
 * @author Steve Ebersole
 */
public abstract class AbstractClassJavaType<T> implements BasicJavaType<T>, Serializable {
	private final Class<T> type;
	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> comparator;

	/**
	 * Initialize a type descriptor for the given type.  Assumed immutable.
	 *
	 * @param type The Java type.
	 *
	 * @see #AbstractClassJavaType(Class, MutabilityPlan)
	 */
	protected AbstractClassJavaType(Class<T> type) {
		this( type, ImmutableMutabilityPlan.instance() );
	}

	/**
	 * Initialize a type descriptor for the given type and mutability plan.
	 *
	 * @param type The Java type.
	 * @param mutabilityPlan The plan for handling mutability aspects of the java type.
	 */
	protected AbstractClassJavaType(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		this(
				type,
				mutabilityPlan,
				Comparable.class.isAssignableFrom( type )
						? ComparableComparator.INSTANCE
						: null
		);
	}

	/**
	 * Initialize a type descriptor for the given type, mutability plan and comparator.
	 *
	 * @param type The Java type.
	 * @param mutabilityPlan The plan for handling mutability aspects of the java type.
	 * @param comparator The comparator for handling comparison of values
	 */
	protected AbstractClassJavaType(
			Class<T> type,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator) {
		this.type = type;
		this.mutabilityPlan = mutabilityPlan;
		this.comparator = comparator;
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	public Class<T> getJavaType() {
		return type;
	}

	@Override
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

	protected HibernateException unknownUnwrap(Class<?> conversionType) {
		return JavaTypeHelper.unknownUnwrap( type, conversionType, this );
	}

	protected HibernateException unknownWrap(Class<?> conversionType) {
		return JavaTypeHelper.unknownWrap( conversionType, type, this );
	}
}
