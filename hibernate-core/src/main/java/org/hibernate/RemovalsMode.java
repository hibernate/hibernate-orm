/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;


import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;

import java.util.List;

/**
 * When {@linkplain SessionCheckMode} is enabled, this option controls how
 * to handle entities which are already contained by the persistence context
 * but which are in a removed state (marked for removal, but not yet flushed).
 * <p>
 * The default is {@link #REPLACE}.
 *
 * @see org.hibernate.Session#findMultiple(Class, List, FindOption...)
 * @see org.hibernate.Session#findMultiple(EntityGraph, List , FindOption...)
 *
 * @since 7.2
 */
public enum RemovalsMode implements FindMultipleOption {
	/**
	 * Removed entities are included in the load result.
	 */
	INCLUDE,
	/**
	 * The default.  Removed entities are replaced with {@code null} in the load result.
	 */
	REPLACE
}
