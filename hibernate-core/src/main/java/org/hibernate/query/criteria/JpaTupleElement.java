/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Internal;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

import jakarta.persistence.TupleElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * API extension to the JPA {@link TupleElement} contract
 *
 * @author Steve Ebersole
 */
public interface JpaTupleElement<T> extends TupleElement<T>, JpaCriteriaNode {
	/**
	 * Return the Java type of this tuple element.
	 */
	@SPI(USE)
	@Nullable JavaType<T> getJavaTypeDescriptor();

	/**
	 * Return the Java type of the tuple element.
	 * @throws IllegalStateException if the Java type has not yet been determined
	 */
	@Override
	default @Nonnull Class<T> getJavaType() {
		final var javaType = getJavaTypeIfKnown();
		if ( javaType == null ) {
			throw new IllegalStateException( "Could not determine the Java type of " + getClass().getSimpleName() );
		}
		return javaType;
	}

	/**
	 * Return the Java type, or {@code null} while type inference is incomplete.
	 */
	@Internal
	default @Nullable Class<T> getJavaTypeIfKnown() {
		final var javaType = getJavaTypeDescriptor();
		return javaType == null ? null : javaType.getJavaTypeClass();
	}

	/**
	 * Return the Java type name of this tuple element.
	 */
	default String getJavaTypeName() {
		final var javaType = getJavaTypeDescriptor();
		return javaType == null ? null : javaType.getTypeName();
	}

	/**
	 * Return whether this tuple element has an enum Java type.
	 */
	default boolean isEnum() {
		return getJavaTypeDescriptor() instanceof EnumJavaType;
	}
}
