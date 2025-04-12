/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.spi.NavigablePath;

/**
 * Any element of the domain model which can be used to create an
 * element of a path expression in a query.
 *
 * @param <J> The type of path element this source creates.
 *
 * @since 7.0
 *
 * @author Gavin King
 */
public interface PathSource<J> {
	/**
	 * The name of this thing.
	 *
	 * @apiNote Mainly used in logging and when creating a {@link NavigablePath}.
	 */
	String getPathName();

	/**
	 * The type of path this source creates.
	 */
	DomainType<J> getPathType();

	/**
	 * Find a {@link PathSource} by name relative to this source.
	 *
	 * @param name the name of the path source to find
	 * @return null if the subPathSource is not found
	 * @throws IllegalStateException to indicate that this source cannot be de-referenced
	 */
	PathSource<?> findSubPathSource(String name);

	/**
	 * Find a {@link PathSource} by name relative to this source. If {@code includeSubtypes} is set
	 * to {@code true} and this path source is polymorphic, also try finding subtype attributes.
	 *
	 * @param name the name of the path source to find
	 * @param includeSubtypes flag indicating whether to consider subtype attributes
	 * @return null if the subPathSource is not found
	 * @throws IllegalStateException to indicate that this source cannot be de-referenced
	 */
	PathSource<?> findSubPathSource(String name, boolean includeSubtypes);
}
