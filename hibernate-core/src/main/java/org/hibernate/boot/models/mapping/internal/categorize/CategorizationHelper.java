/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import jakarta.persistence.Access;
import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.annotations.AnyKeyJdbcType;
import org.hibernate.annotations.AnyKeyJdbcTypeCode;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.EmbeddableInstantiator;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.Type;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.MultipleAttributeNaturesException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import java.util.EnumSet;

import static org.hibernate.boot.models.mapping.internal.categorize.CategorizationLogging.CATEGORIZATION_LOGGER;

/// Helpers/utilities for categorization
///
/// @since 9.0
/// @author Steve Ebersole
public class CategorizationHelper {
	public static boolean isMappedSuperclass(ClassDetails classDetails) {
		return classDetails.hasDirectAnnotationUsage( MappedSuperclass.class );
	}

	public static boolean isEntity(ClassDetails classDetails) {
		return classDetails.hasDirectAnnotationUsage( Entity.class );
	}

	public static boolean isIdentifiable(ClassDetails classDetails) {
		return isEntity( classDetails ) || isMappedSuperclass( classDetails );
	}

	/// Whether this member be used as an indicator for the default access-type for a hierarchy.
	public static boolean isDefaultAccessTypeIndicator(MemberDetails memberDetails) {
		if ( !memberDetails.isPersistable() ) {
			return false;
		}
		if ( memberDetails.hasDirectAnnotationUsage( Access.class ) ) {
			return false;
		}
		if ( memberDetails.hasDirectAnnotationUsage( Transient.class ) ) {
			return false;
		}

		// todo : add a method to cleanly iterate (not Consumer-based) annotations to OrmAnnotationHelper
		//		to implement the fully correct approach to look for any mapping annotation.
		// NOTE: when we do this, be sure to distinguish actual mapping annotations; e.g. @Basic, but not @PostPersist

		// for now, do the legacy bit and just look for @Id and @EmbeddedId
		return memberDetails.hasDirectAnnotationUsage( Id.class )
				|| memberDetails.hasDirectAnnotationUsage( EmbeddedId.class );
	}

	/// Determine the attribute's nature - is it a basic mapping, an embeddable, ...?
	/// Also performs some simple validation around multiple natures being indicated
	static AttributeNature determineAttributeNature(
			MemberDetails backingMember,
			TypeDetails memberType) {
		final EnumSet<AttributeNature> natures = EnumSet.noneOf( AttributeNature.class );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// first, look for explicit nature annotations

		final Any any = backingMember.getDirectAnnotationUsage( HibernateAnnotations.ANY );
		final Basic basic = backingMember.getDirectAnnotationUsage( JpaAnnotations.BASIC );
		final ElementCollection elementCollection = backingMember.getDirectAnnotationUsage(	JpaAnnotations.ELEMENT_COLLECTION );
		final Embedded embedded = backingMember.getDirectAnnotationUsage( JpaAnnotations.EMBEDDED );
		final EmbeddedId embeddedId = backingMember.getDirectAnnotationUsage( JpaAnnotations.EMBEDDED_ID );
		final ManyToAny manyToAny = backingMember.getDirectAnnotationUsage( HibernateAnnotations.MANY_TO_ANY );
		final ManyToMany manyToMany = backingMember.getDirectAnnotationUsage( JpaAnnotations.MANY_TO_MANY );
		final ManyToOne manyToOne = backingMember.getDirectAnnotationUsage( JpaAnnotations.MANY_TO_ONE );
		final OneToMany oneToMany = backingMember.getDirectAnnotationUsage( JpaAnnotations.ONE_TO_MANY );
		final OneToOne oneToOne = backingMember.getDirectAnnotationUsage( JpaAnnotations.ONE_TO_ONE );

		if ( basic != null ) {
			natures.add( AttributeNature.BASIC );
		}

		if ( embeddedId != null
				|| ( embedded != null && elementCollection == null )
				|| ( memberType != null
						&& !backingMember.isPlural()
						&& memberType.determineRawClass().hasDirectAnnotationUsage( Embeddable.class ) ) ) {
			natures.add( AttributeNature.EMBEDDED );
		}

		if ( any != null ) {
			natures.add( AttributeNature.ANY );
		}

		if ( oneToOne != null
				|| manyToOne != null ) {
			natures.add( AttributeNature.TO_ONE );
		}

		final boolean plural;
		if ( oneToMany != null ) {
			plural = true;
			natures.add( AttributeNature.ONE_TO_MANY );
		}
		else if ( manyToMany != null ) {
			plural = true;
			natures.add( AttributeNature.MANY_TO_MANY );
		}
		else if ( elementCollection != null ) {
			plural = true;
			natures.add( AttributeNature.ELEMENT_COLLECTION );
		}
		else if ( manyToAny != null ) {
			plural = true;
			natures.add( AttributeNature.MANY_TO_ANY );
		}
		else {
			plural = false;
		}

		// look at annotations that imply a nature
		//		NOTE : these could apply to the element or index of collection, so
		//		only do these if it is not a collection

		if ( !plural && any == null ) {
			// first implicit basic nature
			if ( backingMember.hasDirectAnnotationUsage( Temporal.class )
					|| backingMember.hasDirectAnnotationUsage( Lob.class )
					|| backingMember.hasDirectAnnotationUsage( Enumerated.class )
					|| hasBasicConversion( backingMember )
					|| backingMember.hasDirectAnnotationUsage( Version.class )
					|| backingMember.hasDirectAnnotationUsage( Generated.class )
					|| backingMember.hasDirectAnnotationUsage( Nationalized.class )
					|| backingMember.hasDirectAnnotationUsage( TimeZoneColumn.class )
					|| backingMember.hasDirectAnnotationUsage( TimeZoneStorage.class )
					|| backingMember.hasDirectAnnotationUsage( Type.class )
					|| backingMember.hasDirectAnnotationUsage( TenantId.class )
					|| backingMember.hasDirectAnnotationUsage( JavaType.class )
					|| backingMember.hasDirectAnnotationUsage( JdbcTypeCode.class )
					|| backingMember.hasDirectAnnotationUsage( JdbcType.class ) ) {
				natures.add( AttributeNature.BASIC );
			}

			// then embedded
			if ( backingMember.hasDirectAnnotationUsage( EmbeddableInstantiator.class )
					|| backingMember.hasDirectAnnotationUsage( CompositeType.class ) ) {
				natures.add( AttributeNature.EMBEDDED );
			}

			// and any
			if ( backingMember.hasDirectAnnotationUsage( AnyDiscriminator.class )
					|| backingMember.hasDirectAnnotationUsage( AnyDiscriminatorValue.class )
					|| backingMember.hasDirectAnnotationUsage( AnyDiscriminatorValues.class )
					|| backingMember.hasDirectAnnotationUsage( AnyKeyJavaType.class )
					|| backingMember.hasDirectAnnotationUsage( AnyKeyJavaClass.class )
					|| backingMember.hasDirectAnnotationUsage( AnyKeyJdbcTypeCode.class )
					|| backingMember.hasDirectAnnotationUsage( AnyKeyJdbcType.class ) ) {
				natures.add( AttributeNature.ANY );
			}
		}

		int size = natures.size();
		return switch ( size ) {
			case 0 -> {
				CATEGORIZATION_LOGGER.debugf(
						"Implicitly interpreting attribute `%s` as BASIC",
						backingMember.resolveAttributeName()
				);
				yield AttributeNature.BASIC;
			}
			case 1 -> natures.iterator().next();
			default -> throw new MultipleAttributeNaturesException( backingMember.resolveAttributeName(), natures );
		};
	}

	private static boolean hasBasicConversion(MemberDetails backingMember) {
		final Convert convert = backingMember.getDirectAnnotationUsage( Convert.class );
		return convert != null && ( convert.attributeName() == null || convert.attributeName().isEmpty() );
	}
}
