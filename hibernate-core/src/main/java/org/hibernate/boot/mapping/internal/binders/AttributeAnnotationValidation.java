package org.hibernate.boot.mapping.internal.binders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import org.hibernate.AnnotationException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.MapKeyFormula;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.mapping.internal.model.AggregateMappingIntent;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.models.spi.MemberDetails;

/// Validates annotations shared by entity attributes and embedded-site members.
///
/// @author Steve Ebersole
final class AttributeAnnotationValidation {
	private AttributeAnnotationValidation() {
	}

	static void validate(MemberDetails member, String path, AttributeNature nature, boolean embedded) {
		final boolean collection = switch ( nature ) {
			case ELEMENT_COLLECTION, ONE_TO_MANY, MANY_TO_MANY, MANY_TO_ANY -> true;
			default -> false;
		};
		if ( member.hasDirectAnnotationUsage( Cache.class ) && !collection ) {
			throw new AnnotationException( "Property '" + path + "' is annotated '@Cache' but is not a collection" );
		}
		if ( member.hasDirectAnnotationUsage( Column.class )
				&& (embedded || member.hasDirectAnnotationUsage( CompositeType.class ))
				&& !AggregateMappingIntent.hasAggregateJdbcTypeCode( member )
				&& !member.hasDirectAnnotationUsage( Struct.class )
				&& !member.getType().determineRawClass().hasDirectAnnotationUsage( Struct.class ) ) {
			throw new AnnotationException( "Property '" + path
					+ "' may not be annotated '@Column'; use '@AttributeOverride' to specify its columns" );
		}
		if ( member.hasDirectAnnotationUsage( MapKeyFormula.class ) ) {
			final String conflict;
			if ( member.hasDirectAnnotationUsage( MapKey.class ) ) {
				throw new AnnotationException( "Property '" + path + "' is annotated both '@MapKey' and '@MapKeyFormula'" );
			}
			else if ( member.hasDirectAnnotationUsage( MapKeyColumn.class ) ) { conflict = "@MapKeyColumn"; }
			else if ( member.hasDirectAnnotationUsage( MapKeyJoinColumn.class )
					|| member.hasDirectAnnotationUsage( MapKeyJoinColumns.class ) ) { conflict = "@MapKeyJoinColumn"; }
			else { conflict = null; }
			if ( conflict != null ) {
				throw new AnnotationException( "Property '" + path + "' is annotated both '@MapKeyFormula' and '" + conflict + "'" );
			}
			if ( member.getMapKeyType() != null && member.getMapKeyType().determineRawClass().hasDirectAnnotationUsage( Entity.class ) ) {
				throw new AnnotationException( "Property '" + path + "' is annotated '@MapKeyFormula' but its key is an entity" );
			}
		}
	}
}
