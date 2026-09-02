/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.SPI;
import org.hibernate.internal.util.compare.ComparableComparator;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Base adapter for a basic [JavaType] represented by a [Class].
///
/// @param <T> the represented Java value type
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class AbstractClassJavaType<T> implements BasicJavaType<T>, Serializable {
	private final Class<T> type;
	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> comparator;

	/// Initialize an immutable descriptor for the given Java class.
	/// @see #AbstractClassJavaType(Class, MutabilityPlan)
	@SPI(IMPLEMENT)
	protected AbstractClassJavaType(Class<T> type) {
		this( type, ImmutableMutabilityPlan.instance() );
	}

	/// Initialize a descriptor with explicit mutability semantics.
	@SuppressWarnings("unchecked")
	@SPI(IMPLEMENT)
	protected AbstractClassJavaType(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		this(
				type,
				mutabilityPlan,
				Comparable.class.isAssignableFrom( type )
						? ComparableComparator.INSTANCE
						: null
		);
	}

	/// Initialize a descriptor with explicit mutability and comparison semantics.
	@SPI(IMPLEMENT)
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
	public final Class<T> getJavaTypeClass() {
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
