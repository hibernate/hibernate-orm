/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.internal;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.filter.internal.FilterHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.hibernate.internal.util.StringHelper.replace;

/**
 * Utility methods for dealing with {@link SqlFragmentAlias}
 * placeholders occurring in {@linkplain Filter filter
 * conditions} and {@linkplain SQLRestriction restrictions}.
 *
 * @see FilterHelper
 * @see SqlRestriction
 */
public class SqlFragmentAliasHelper {

	private SqlFragmentAliasHelper() {
	}

	/**
	 * Interpolates the {@code {alias}} placeholders of a SQL fragment with the aliases
	 * of the tables they refer to, as resolved by the given {@link FilterAliasGenerator},
	 * registering an {@linkplain EntityNameUse#EXPRESSION expression use} of the entity
	 * mapped to each referenced table so that its table is retained in the generated SQL.
	 * Placeholders whose table cannot be resolved to an alias are left in place.
	 *
	 * @param fragment a SQL fragment containing {@code {alias}} placeholders
	 * @param aliasTableMap the placeholder aliases mapped to the tables they refer to
	 * @param entityNameByTableName table names mapped to entity names, or {@code null}
	 */
	public static String interpolateAliases(
			String fragment,
			Map<String, String> aliasTableMap,
			FilterAliasGenerator aliasGenerator,
			Map<String, String> entityNameByTableName,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		String result = fragment;
		for ( var entry : aliasTableMap.entrySet() ) {
			final String tableName = entry.getValue();
			final String tableAlias = aliasGenerator.getAlias( tableName );
			if ( tableAlias != null ) {
				final String interpolated = replace( result, "{" + entry.getKey() + "}", tableAlias );
				if ( creationState != null
						&& entityNameByTableName != null
						&& !interpolated.equals( result ) ) {
					final String entityName = entityNameByTableName.get( tableName );
					if ( entityName != null ) {
						creationState.registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION, entityName );
					}
				}
				result = interpolated;
			}
		}
		return result;
	}

	/**
	 * Removes the {@code {alias}.} qualifiers of a SQL fragment, for use in
	 * single-table contexts where no alias can be interpolated.
	 */
	public static String stripAliasQualifiers(String fragment, Map<String, String> aliasTableMap) {
		String result = fragment;
		for ( String alias : aliasTableMap.keySet() ) {
			result = replace( result, "{" + alias + "}.", "" );
		}
		return result;
	}

	/**
	 * Resolves an alias-to-table map and an alias-to-entity-name map into a single
	 * alias-to-table map, resolving each entity to its primary table.
	 */
	public static Map<String, String> resolveAliasTableMap(
			Map<String, String> aliasTableMap,
			Map<String, String> aliasEntityMap,
			SessionFactoryImplementor factory) {
		if ( ( aliasTableMap == null || aliasTableMap.isEmpty() )
				&& ( aliasEntityMap == null || aliasEntityMap.isEmpty() ) ) {
			return emptyMap();
		}
		final Map<String, String> result = new HashMap<>();
		if ( aliasTableMap != null ) {
			result.putAll( aliasTableMap );
		}
		if ( aliasEntityMap != null ) {
			for ( var entry : aliasEntityMap.entrySet() ) {
				final var entityDescriptor =
						factory.getMappingMetamodel()
								.getEntityDescriptor( entry.getValue() );
				result.put( entry.getKey(), entityDescriptor.getTableName() );
			}
		}
		return result;
	}
}
