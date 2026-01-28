/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import jakarta.persistence.StatementReference;

import java.util.List;

/// Extension to the JPA {@linkplain StatementReference} contract.
/// Provides some simple default implementations for methods which
/// we don't care about internally.
/// Also acts as a marker for Hibernate implementors.
///
/// @author Steve Ebersole
///
/// @since 8.0
public interface JpaStatementReference<T> extends JpaReference, StatementReference {

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
