/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;


import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;

import java.util.List;

/**
 * Indicates whether the persistence context should be checked for entities
 * matching the identifiers to be loaded - <ul>
 *     <li>Entities which are in a managed state are not re-loaded from the database.
 *     		those identifiers are removed from the SQL restriction sent to the database.
 *     <li>Entities which are in a removed state are {@linkplain RemovalsMode#REPLACE excluded}
 *     		from the result by default, but can be {@linkplain RemovalsMode#INCLUDE included} if desired.
 * </ul>
 * <p/>
 * The default is {@link #DISABLED}
 *
 * @see org.hibernate.Session#findMultiple(Class, List , FindOption...)
 * @see org.hibernate.Session#findMultiple(EntityGraph, List , FindOption...)
 *
 * @since 7.2
 */
@Incubating
public enum SessionCheckMode implements FindMultipleOption {
	/**
	 * The persistence context will be checked.  Identifiers for entities already contained
	 * in the persistence context will not be sent to the database for loading.  If the
	 * entity is marked for removal in the persistence context, whether it is returned
	 * is controlled by {@linkplain RemovalsMode}.
	 *
	 * @see RemovalsMode
	 */
	ENABLED,
	/**
	 * The default.  All identifiers to be loaded will be read from the database and returned.
	 */
	DISABLED
}
