/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.metamodel.Type;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;

/**
 * The target for cast.
 *
 * @since 7.0
 */
@Incubating
public interface JpaCastTarget<T> {

	/**
	 * Returns the JPA type for this cast target.
	 */
	Type<T> getType();

	/**
	 * Returns the specified length of the cast target or {@code null}.
	 */
	@Nullable Long getLength();

	/**
	 * Returns the specified precision of the cast target or {@code null}.
	 */
	@Nullable Integer getPrecision();

	/**
	 * Returns the specified scale of the cast target or {@code null}.
	 */
	@Nullable Integer getScale();
}
