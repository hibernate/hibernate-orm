/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;


import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;

import java.util.List;

/**
 * When {@linkplain SessionChecking} is enabled, this option controls how
 * to handle entities which are already contained by the persistence context
 * but which are in a removed state (marked for removal, but not yet flushed).
 * <p>
 * The default is {@link #EXCLUDE}.
 *
 * @see org.hibernate.Session#findMultiple(Class, List, FindOption...)
 * @see org.hibernate.Session#findMultiple(EntityGraph, List , FindOption...)
 *
 * @since 7.2
 */
public enum IncludeRemovals implements MultiFindOption {
	/**
	 * Removed entities are included in the load result.
	 */
	INCLUDE,
	/**
	 * The default.  Removed entities are excluded from the load result.
	 * <p/>
	 * When combined with {@linkplain OrderedReturn#UNORDERED}, the entity is
	 * simply excluded from the result.
	 * <p/>
	 * When combined with {@linkplain OrderedReturn#ORDERED}, the entity is replaced
	 * by {@code null} in the result.
	 *
	 * @see OrderedReturn
	 */
	EXCLUDE
}
