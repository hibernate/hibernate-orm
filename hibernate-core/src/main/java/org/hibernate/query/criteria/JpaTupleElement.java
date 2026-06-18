/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.TupleElement;
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
	@Nullable JavaType<T> getJavaTypeDescriptor();

	@Override
	default @Nullable Class<T> getJavaType() {
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
