/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.TypedQueryReference;

import java.util.List;

/// Extension to the JPA {@linkplain TypedQueryReference} contract.
/// Provides some simple default implementations for methods which
/// we don't care about internally.
/// Also acts as a marker for Hibernate implementors.
///
/// @author Steve Ebersole
public interface JpaTypedQueryReference<R> extends JpaReference, TypedQueryReference<R> {

	/// {@inheritDoc}
	@Override
	default List<Class<?>> getParameterTypes() {
		return null;
	}

	/// {@inheritDoc}
	@Override
	default List<String> getParameterNames() {
		return null;
	}

	/// {@inheritDoc}
	@Override
	default List<Object> getArguments() {
		return null;
	}
}
