/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import java.util.HashMap;
import java.util.Map;
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
	private RestrictionRendering() {
	}

	public static String render(
			String template,
			String defaultAlias,
			boolean useQualifier,
			EntityMappingType entityMapping,
			TableGroup tableGroup,
			SqlAstCreationState creationState) {
		final Map<String, String> tables = new HashMap<>();
		final Map<String, String> entities = new HashMap<>();
		entityMapping.forEachAttributeMapping( attribute -> {
			if ( !attribute.isPluralAttributeMapping() ) {
				attribute.forEachSelectable( (index, selectable) -> {
					if ( !selectable.isFormula() ) {
						final String column = selectable.getSelectionExpression();
						final String table = selectable.getContainingTableExpression();
						// Ambiguous column names keep the mapping's original qualification.
						tables.merge( column, table, (first, second) -> first.equals( second ) ? first : "" );
						final var declaringEntity = attribute.findContainingEntityMapping();
						if ( declaringEntity != null ) {
							entities.put( column, declaringEntity.getEntityName() );
						}
					}
				} );
			}
		} );
		String result = template;
		for ( var entry : tables.entrySet() ) {
			final String column = entry.getKey();
			final String table = entry.getValue();
			if ( table != null && !table.isEmpty() ) {
				final boolean quoted = column.startsWith( "\"" ) || column.startsWith( "`" ) || column.startsWith( "[" );
				final var pattern = Pattern.compile(
						Pattern.quote( Template.TEMPLATE + "." + column ) + "(?![\\p{L}\\p{N}_$])",
						quoted ? 0 : Pattern.CASE_INSENSITIVE );
				if ( !pattern.matcher( result ).find() ) {
					continue;
				}
				final var reference = tableGroup.resolveTableReference( table );
				final String alias = !useQualifier || reference.getIdentificationVariable() == null
						? reference.getTableId() : reference.getIdentificationVariable();
				result = pattern.matcher( result ).replaceAll( Matcher.quoteReplacement( alias + "." + column ) );
				if ( creationState != null && entities.containsKey( column ) ) {
					creationState.registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION, entities.get( column ) );
				}
			}
		}
		return replace( result, Template.TEMPLATE, defaultAlias );
	}
}
