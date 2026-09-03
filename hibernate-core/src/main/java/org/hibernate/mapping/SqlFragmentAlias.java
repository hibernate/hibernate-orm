/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A mapping model object representing a
 * {@linkplain org.hibernate.annotations.SqlFragmentAlias sql fragment alias},
 * which binds an {@code {alias}} placeholder occurring in a
 * {@linkplain SQLRestriction restriction} to a table,
 * named either directly or via the entity mapped to it.
 *
 * @since 8.1
 */
public record SqlFragmentAlias(String alias, String table, String entityName) implements Serializable {
	/**
	 * Extracts mapping model objects from the given
	 * {@link org.hibernate.annotations.SqlFragmentAlias} annotations,
	 * omitting any alias which specifies neither a table nor an entity.
	 */
	public static List<SqlFragmentAlias> from(org.hibernate.annotations.SqlFragmentAlias[] aliases) {
		final List<SqlFragmentAlias> result = new ArrayList<>( aliases.length );
		for ( var aliasAnnotation : aliases ) {
			final String table = aliasAnnotation.table().isBlank() ? null : aliasAnnotation.table();
			final var entityClass = aliasAnnotation.entity();
			final String entityName = entityClass == void.class ? null : entityClass.getName();
			if ( table != null || entityName != null ) {
				result.add( new SqlFragmentAlias( aliasAnnotation.alias(), table, entityName ) );
			}
		}
		return result;
	}
}
