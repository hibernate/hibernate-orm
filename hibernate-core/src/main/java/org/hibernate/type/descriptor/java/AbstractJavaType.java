/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.SPI;
import org.hibernate.internal.util.compare.ComparableComparator;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Base adapter for a basic [JavaType] identified by a reflective [Type].
///
/// Use this base when the represented Java type is not most naturally modeled
/// by a `Class`; otherwise prefer [AbstractClassJavaType].
///
/// @param <T> the represented Java value type
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public abstract class AbstractJavaType<T> implements BasicJavaType<T>, Serializable {
	private final Type type;
	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> comparator;

	/// Initialize an immutable descriptor for the given Java type.
	/// @see #AbstractJavaType(Type, MutabilityPlan)
	@SPI(IMPLEMENT)
	protected AbstractJavaType(Type type) {
		this( type, ImmutableMutabilityPlan.instance() );
	}

	/// Initialize a descriptor with explicit mutability semantics.
	@SuppressWarnings("unchecked")
	@SPI(IMPLEMENT)
	protected AbstractJavaType(Type type, MutabilityPlan<T> mutabilityPlan) {
		this.type = type;
		this.mutabilityPlan = mutabilityPlan;
		this.comparator =
				type != null && Comparable.class.isAssignableFrom( getJavaTypeClass() )
						? ComparableComparator.INSTANCE
						: null;
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public Type getJavaType() {
		return type;
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
		throw new HibernateException(
				"Unknown unwrap conversion requested: " + type.getTypeName() + " to " + conversionType.getName()
		);
	}

	protected HibernateException unknownWrap(Class<?> conversionType) {
		throw new HibernateException(
				"Unknown wrap conversion requested: " + conversionType.getName() + " to " + type.getTypeName()
		);
	}
}
