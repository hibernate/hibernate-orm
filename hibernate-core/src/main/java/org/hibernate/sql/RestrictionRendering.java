/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.TableGroup;

import static org.hibernate.internal.util.StringHelper.replace;

/** Qualifies mapped columns in SQL restrictions using the tables that actually declare them. */
@org.hibernate.Internal
public final class RestrictionRendering {
	private final String template;
	private final Column[] columns;

	private record Column(String name, String table, String entity, Pattern pattern) {
	}

	private record Owner(String column, String table, String entity) {
	}

	private RestrictionRendering(String template, Column[] columns) {
		this.template = template;
		this.columns = columns;
	}

	/** Compiles mapping-dependent column matching once, without retaining any query aliases. */
	public static RestrictionRendering compile(String template, EntityMappingType entityMapping) {
		final Map<String, Owner> owners = new HashMap<>();
		entityMapping.forEachAttributeMapping( attribute -> {
			if ( !attribute.isPluralAttributeMapping() ) {
				attribute.forEachSelectable( (index, selectable) -> {
					if ( !selectable.isFormula() ) {
						final String column = selectable.getSelectionExpression();
						final var declaringEntity = attribute.findContainingEntityMapping();
						final var owner = new Owner( column, selectable.getContainingTableExpression(),
								declaringEntity == null ? null : declaringEntity.getEntityName() );
						// Keep the original qualification when either table or entity ownership is ambiguous.
						owners.merge( isQuoted( column ) ? column : column.toLowerCase( Locale.ROOT ), owner,
								(first, second) -> Objects.equals( first.table, second.table )
										&& Objects.equals( first.entity, second.entity )
										? first : new Owner( column, null, null ) );
					}
				} );
			}
		} );
		final List<Column> columns = new ArrayList<>();
		for ( var owner : owners.values() ) {
			if ( owner.table != null ) {
				final var pattern = Pattern.compile(
						Pattern.quote( Template.TEMPLATE + "." + owner.column ) + "(?![\\p{L}\\p{N}_$])",
						isQuoted( owner.column ) ? 0 : Pattern.CASE_INSENSITIVE );
				if ( pattern.matcher( template ).find() ) {
					columns.add( new Column( owner.column, owner.table, owner.entity, pattern ) );
				}
			}
		}
		return new RestrictionRendering( template, columns.toArray( Column[]::new ) );
	}

	private static boolean isQuoted(String column) {
		return column.startsWith( "\"" ) || column.startsWith( "`" ) || column.startsWith( "[" );
	}

	public String render(
			String defaultAlias,
			boolean useQualifier,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		String result = template;
		for ( var column : columns ) {
			final var reference = tableGroup.resolveTableReference( column.table );
			final String alias = !useQualifier || reference.getIdentificationVariable() == null
					? reference.getTableId() : reference.getIdentificationVariable();
			result = column.pattern.matcher( result ).replaceAll( Matcher.quoteReplacement( alias + "." + column.name ) );
			if ( creationState != null && column.entity != null ) {
				creationState.registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION, column.entity );
			}
		}
		return replace( result, Template.TEMPLATE, defaultAlias );
	}
}
