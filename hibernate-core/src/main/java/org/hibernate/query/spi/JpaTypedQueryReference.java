/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.TypedQueryReference;

import java.util.List;

/**
 * Extension to {@linkplain TypedQueryReference} to handle the fact
 * that every concrete implementation of this contract in Hibernate
 * will return null for a number of the defined methods.
 *
 * @author Steve Ebersole
 */
public interface JpaTypedQueryReference<R> extends TypedQueryReference<R> {
	@Override
	default List<Class<?>> getParameterTypes() {
		return null;
	}

	@Override
	default List<String> getParameterNames() {
		return null;
	}

	@Override
	default List<Object> getArguments() {
		return null;
	}
}
