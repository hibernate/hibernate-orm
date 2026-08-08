/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.internal;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.SqlFragmentAlias;
import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.persister.filter.internal.FilterHelper;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.emptyMap;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.replace;
import static org.hibernate.persister.internal.SqlFragmentAliasHelper.interpolateAliases;
import static org.hibernate.persister.internal.SqlFragmentAliasHelper.stripAliasQualifiers;
import static org.hibernate.sql.Template.renderWhereStringTemplate;

/**
 * Encapsulates the rendering of a {@linkplain SQLRestriction
 * where restriction}, the static counterpart to the
 * {@linkplain FilterHelper handling of filters}.
 * <p>
 * A restriction is rendered in one of two modes:
 * <ul>
 * <li>without {@linkplain SqlFragmentAlias aliases}, every
 *     unqualified column reference is qualified with the alias of a single table,
 *     determined by the caller; or
 * <li>with aliases, each {@code {alias}} placeholder is interpolated with the alias of
 *     the table it refers to, which need not be the primary table of the restricted
 *     element.
 * </ul>
 *
 * @see SQLRestriction#aliases()
 */
public class SqlRestriction {

	private final String fragment;
	private final String template;
	private final Map<String, String> aliasTableMap;
	private final Map<String, String> entityNameByTableName;
	private final String singleTableFragment;

	private SqlRestriction(
			String fragment,
			String template,
			Map<String, String> aliasTableMap,
			Map<String, String> entityNameByTableName,
			String singleTableFragment) {
		this.fragment = fragment;
		this.template = template;
		this.aliasTableMap = aliasTableMap;
		this.entityNameByTableName = entityNameByTableName;
		this.singleTableFragment = singleTableFragment;
	}

	/**
	 * Creates a restriction, or returns {@code null} if the given clause is empty.
	 *
	 * @param clause the restriction, as specified in the mapping
	 * @param aliases the {@linkplain SqlFragmentAlias
	 *        alias placeholders} of the restriction, possibly {@code null}
	 * @param entityNameByTableNameSupplier table names mapped to entity names,
	 *        resolved only for an alias-based restriction
	 */
	public static SqlRestriction create(
			String clause,
			List<SqlFragmentAlias> aliases,
			Supplier<Map<String, String>> entityNameByTableNameSupplier,
			SessionFactoryImplementor factory) {
		if ( isEmpty( clause ) ) {
			return null;
		}
		final String fragment = "(" + clause + ")";
		final var aliasTableMap = resolveAliasTableMap( aliases, factory );
		if ( aliasTableMap.isEmpty() ) {
			final String template = renderWhereStringTemplate(
				fragment,
				factory.getJdbcServices().getDialect(),
				factory.getTypeConfiguration()
			);
			return new SqlRestriction( fragment, template, aliasTableMap, null, fragment );
		}
		else {
			return new SqlRestriction(
				fragment,
				null,
				aliasTableMap,
				entityNameByTableNameSupplier.get(),
				// single-table contexts cannot interpolate table aliases,
				// so fall back to unqualified column references there
				stripAliasQualifiers( fragment, aliasTableMap )
			);
		}
	}

	/**
	 * Resolves each alias placeholder to the name of the table it refers to,
	 * resolving an alias given as an entity to the entity's primary table.
	 */
	private static Map<String, String> resolveAliasTableMap(
			List<SqlFragmentAlias> aliases,
			SessionFactoryImplementor factory) {
		if ( aliases == null || aliases.isEmpty() ) {
			return emptyMap();
		}
		final Map<String, String> result = new HashMap<>();
		for ( var alias : aliases ) {
			result.put( alias.alias(), alias.entityName() != null
				? factory.getMappingMetamodel()
					.getEntityDescriptor( alias.entityName() )
					.getTableName()
				: alias.table() );
		}
		return result;
	}

	public boolean isAliasBased() {
		return template == null;
	}

	/**
	 * The restriction for use in single-table contexts, with alias
	 * placeholders removed if the restriction is alias-based.
	 */
	public String getSingleTableFragment() {
		return singleTableFragment;
	}

	/**
	 * Renders a non-alias-based restriction, qualifying every unqualified
	 * column reference with the given alias.
	 */
	public String render(String alias) {
		return replace( template, Template.TEMPLATE, alias );
	}

	/**
	 * Renders an alias-based restriction, interpolating each {@code {alias}}
	 * placeholder with the alias of the table it refers to.
	 */
	public String render(
			FilterAliasGenerator aliasGenerator,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		return interpolateAliases(
			fragment,
			aliasTableMap,
			aliasGenerator,
			entityNameByTableName,
			tableGroup,
			creationState
		);
	}
}
