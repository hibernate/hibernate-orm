/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
		return getJavaTypeDescriptor() == null ? null : getJavaTypeDescriptor().getJavaTypeClass();
	}

	default String getJavaTypeName() {
		return getJavaTypeDescriptor() == null ? null : getJavaTypeDescriptor().getTypeName();
	}

	default boolean isEnum() {
		return getJavaTypeDescriptor() instanceof EnumJavaType;
	}
}
