/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.TupleElement;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * API extension to the JPA {@link TupleElement} contract
 *
 * @author Steve Ebersole
 */
public interface JpaTupleElement<T> extends TupleElement<T>, JpaCriteriaNode {
	@Nullable JavaType<T> getJavaTypeDescriptor();

	@Override
	default @Nullable Class<? extends T> getJavaType() {
		// todo (6.0) : can this signature just return `Class<T>`?
		final JavaType<T> javaType = getJavaTypeDescriptor();
		return javaType == null ? null : javaType.getJavaTypeClass();
	}

	default String getJavaTypeName() {
		final JavaType<T> javaType = getJavaTypeDescriptor();
		return javaType == null ? null : javaType.getTypeName();
	}

	default boolean isEnum() {
		return getJavaTypeDescriptor() instanceof EnumJavaType;
	}
}
