/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import jakarta.persistence.metamodel.Bindable;
import org.hibernate.spi.NavigablePath;

public interface PathSource<J> {
	/**
	 * The name of this thing.
	 *
	 * @apiNote Mainly used in logging and when creating a {@link NavigablePath}.
	 */
	String getPathName();

	/**
	 * The type of path this source creates.
	 *
	 * @apiNote Analogous to {@link Bindable#getBindableJavaType()}.
	 */
	DomainType<J> getSqmPathType();

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
	default PathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
		return findSubPathSource( name );
	}
}
