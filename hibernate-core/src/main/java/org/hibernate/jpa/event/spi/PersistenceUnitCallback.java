/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.spi;

import java.io.Serializable;

import jakarta.annotation.Nonnull;

/// Represents a JPA persistence unit lifecycle callback method.
///
/// @author Gavin King
///
/// @since 8.0
public interface PersistenceUnitCallback<T> extends Serializable {
	@Nonnull
	PersistenceUnitCallbackType getCallbackType();

	@Nonnull
	Class<T> getCallbackTargetType();

	void performCallback(@Nonnull T target);
}
