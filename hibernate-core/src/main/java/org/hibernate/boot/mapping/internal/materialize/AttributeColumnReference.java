/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.boot.spi.MetadataBuildingContext;

/// Resolves entity attribute fallbacks and explicit collection-role references.
///
/// @param table Table owning the index or unique constraint
/// @param context Boot context containing the completed entity mappings
/// @param entityName Entity declaring the table annotation
/// @param sourceRole Annotation location used for diagnostics
/// @param collectionRole Collection owning explicit role references, or null for entity declarations
/// @author Steve Ebersole
record AttributeColumnReference(Table table, MetadataBuildingContext context, String entityName, String sourceRole, String collectionRole) {

	static boolean isRoleReference(String reference) {
		return reference.startsWith( "{element}" ) || reference.startsWith( "{key}" );
	}

	Column resolveRole(String reference) {
		if ( collectionRole == null ) {
			throw invalid( reference, "collection role marker requires a collection-table or join-table declaration" );
		}
		final var collection = context.getMetadataCollector().getCollectionBinding( collectionRole );
		if ( collection == null ) { throw new MappingException( "Missing declaration collection: " + collectionRole ); }
		final boolean key = reference.startsWith( "{key}" );
		final String marker = key ? "{key}" : "{element}";
		Value value;
		if ( key ) {
			if ( !(collection instanceof org.hibernate.mapping.Map map) ) {
				throw invalid( reference, "{key} requires a map collection" );
			}
			value = map.getIndex();
		}
		else { value = collection.getElement(); }
		if ( reference.length() > marker.length() ) {
			if ( reference.charAt( marker.length() ) != '.' ) { throw invalid( reference, "malformed collection role path" ); }
			for ( var segment : reference.substring( marker.length() + 1 ).split( "\\.", -1 ) ) {
				if ( segment.isEmpty() ) { throw invalid( reference, "malformed collection role path" ); }
				if ( !(value instanceof Component component) ) {
					throw invalid( reference, "path traversal is only supported through embeddables" );
				}
				try { value = component.getProperty( segment ).getValue(); }
				catch ( MappingException missing ) { throw invalid( reference, "unknown collection attribute '" + segment + "'" ); }
			}
		}
		return requireColumn( value, reference );
	}

	Column resolve(String reference) {
		final var entity = context.getMetadataCollector()
				.getEntityBinding( entityName );
		if ( entity == null ) {
			throw new MappingException( "Missing declaration entity: " + entityName );
		}
		final var segments = reference.split( "\\.", -1 );
		for ( var segment : segments ) {
			if ( segment.isEmpty() ) { return null; }
		}
		Property property;
		try {
			property = entity.getRecursiveProperty( segments[0] );
		}
		catch ( MappingException missing ) {
			return null;
		}
		for ( int i = 1; i < segments.length; i++ ) {
			if ( !(property.getValue() instanceof Component component) ) {
				throw invalid( reference, "path traversal is only supported through embeddables" );
			}
			try {
				property = component.getProperty( segments[i] );
			}
			catch ( MappingException missing ) {
				return null;
			}
		}
		return requireColumn( property.getValue(), reference );
	}

	private Column requireColumn(Value value, String reference) {
		if ( value instanceof OneToOne oneToOne && oneToOne.getMappedByProperty() != null ) {
			throw invalid( reference, "inverse association has no local column" );
		}
		if ( value instanceof org.hibernate.mapping.Collection ) {
			throw invalid( reference, "collection attribute is not a single local column" );
		}
		final var selectables = value.getSelectables();
		if ( selectables.stream().anyMatch( selectable -> selectable.isFormula() ) ) {
			throw invalid( reference, "attribute contains a formula" );
		}
		if ( selectables.size() != 1 ) {
			throw invalid( reference, "attribute must resolve to exactly one column (found "
					+ selectables.size() + ")" );
		}
		final var column = (Column) selectables.get( 0 );
		if ( table.getColumn( column ) != column ) {
			throw invalid( reference, "attribute does not identify an actual column on the declaring table" );
		}
		return column;
	}

	private AnnotationException invalid(String reference, String reason) {
		return new AnnotationException( "Attribute reference '" + reference + "' on table '"
				+ table.getName() + "' at " + sourceRole + ": " + reason );
	}
}
