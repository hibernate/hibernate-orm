/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.EnumSet;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.MultipleAttributeNaturesException;
import org.hibernate.boot.models.categorize.ModelCategorizationLogging;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

/**
 * @author Steve Ebersole
 */
public class CategorizationHelper {
	public static boolean isMappedSuperclass(ClassDetails classDetails) {
		return classDetails.getAnnotationUsage( JpaAnnotations.MAPPED_SUPERCLASS ) != null;
	}

	public static boolean isEntity(ClassDetails classDetails) {
		return classDetails.getAnnotationUsage( JpaAnnotations.ENTITY ) != null;
	}

	public static boolean isIdentifiable(ClassDetails classDetails) {
		return isEntity( classDetails ) || isMappedSuperclass( classDetails );
	}

	public static AccessType determineAccessType(ClassDetails classDetails, AccessType defaultAccessType) {
		final AnnotationUsage<Access> annotation = classDetails.getAnnotationUsage( JpaAnnotations.ACCESS );
		if ( annotation != null ) {
			return annotation.getAttributeValue( "value" );
		}

		return defaultAccessType;
	}

	/**
	 * Determine the attribute's nature - is it a basic mapping, an embeddable, ...?
	 * </p>
	 * Also performs some simple validation around multiple natures being indicated
	 */
	public static AttributeMetadata.AttributeNature determineAttributeNature(MemberDetails backingMember) {
		final EnumSet<AttributeMetadata.AttributeNature> natures = EnumSet.noneOf( AttributeMetadata.AttributeNature.class );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// first, look for explicit nature annotations

		final AnnotationUsage<Any> any = backingMember.getAnnotationUsage( HibernateAnnotations.ANY );
		final AnnotationUsage<Basic> basic = backingMember.getAnnotationUsage( JpaAnnotations.BASIC );
		final AnnotationUsage<ElementCollection> elementCollection = backingMember.getAnnotationUsage( JpaAnnotations.ELEMENT_COLLECTION );
		final AnnotationUsage<Embedded> embedded = backingMember.getAnnotationUsage( JpaAnnotations.EMBEDDED );
		final AnnotationUsage<EmbeddedId> embeddedId = backingMember.getAnnotationUsage( JpaAnnotations.EMBEDDED_ID );
		final AnnotationUsage<ManyToAny> manyToAny = backingMember.getAnnotationUsage( HibernateAnnotations.MANY_TO_ANY );
		final AnnotationUsage<ManyToMany> manyToMany = backingMember.getAnnotationUsage( JpaAnnotations.MANY_TO_MANY );
		final AnnotationUsage<ManyToOne> manyToOne = backingMember.getAnnotationUsage( JpaAnnotations.MANY_TO_ONE );
		final AnnotationUsage<OneToMany> oneToMany = backingMember.getAnnotationUsage( JpaAnnotations.ONE_TO_MANY );
		final AnnotationUsage<OneToOne> oneToOne = backingMember.getAnnotationUsage( JpaAnnotations.ONE_TO_ONE );

		if ( basic != null ) {
			natures.add( AttributeMetadata.AttributeNature.BASIC );
		}

		if ( embedded != null
				|| embeddedId != null
				|| ( backingMember.getType() != null && backingMember.getType().getAnnotationUsage( JpaAnnotations.EMBEDDABLE ) != null ) ) {
			natures.add( AttributeMetadata.AttributeNature.EMBEDDED );
		}

		if ( any != null ) {
			natures.add( AttributeMetadata.AttributeNature.ANY );
		}

		if ( oneToOne != null
				|| manyToOne != null ) {
			natures.add( AttributeMetadata.AttributeNature.TO_ONE );
		}

		final boolean plural = oneToMany != null
				|| manyToMany != null
				|| elementCollection != null
				|| manyToAny != null;
		if ( plural ) {
			natures.add( AttributeMetadata.AttributeNature.PLURAL );
		}

		// look at annotations that imply a nature
		//		NOTE : these could apply to the element or index of collection, so
		//		only do these if it is not a collection

		if ( !plural ) {
			// first implicit basic nature
			if ( backingMember.getAnnotationUsage( JpaAnnotations.TEMPORAL ) != null
					|| backingMember.getAnnotationUsage( JpaAnnotations.LOB ) != null
					|| backingMember.getAnnotationUsage( JpaAnnotations.ENUMERATED ) != null
					|| backingMember.getAnnotationUsage( JpaAnnotations.CONVERT ) != null
					|| backingMember.getAnnotationUsage( JpaAnnotations.VERSION ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.GENERATED ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.NATIONALIZED ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.TZ_COLUMN ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.TZ_STORAGE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.TYPE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.TENANT_ID ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.JAVA_TYPE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.JDBC_TYPE_CODE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.JDBC_TYPE ) != null ) {
				natures.add( AttributeMetadata.AttributeNature.BASIC );
			}

			// then embedded
			if ( backingMember.getAnnotationUsage( HibernateAnnotations.EMBEDDABLE_INSTANTIATOR ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.COMPOSITE_TYPE ) != null ) {
				natures.add( AttributeMetadata.AttributeNature.EMBEDDED );
			}

			// and any
			if ( backingMember.getAnnotationUsage( HibernateAnnotations.ANY_DISCRIMINATOR ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_DISCRIMINATOR_VALUE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_DISCRIMINATOR_VALUES ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_KEY_JAVA_TYPE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_KEY_JAVA_CLASS ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_KEY_JDBC_TYPE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_KEY_JDBC_TYPE_CODE ) != null ) {
				natures.add( AttributeMetadata.AttributeNature.ANY );
			}
		}

		int size = natures.size();
		return switch ( size ) {
			case 0 -> {
				ModelCategorizationLogging.MODEL_CATEGORIZATION_LOGGER.debugf(
						"Implicitly interpreting attribute `%s` as BASIC",
						backingMember.resolveAttributeName()
				);
				yield AttributeMetadata.AttributeNature.BASIC;
			}
			case 1 -> natures.iterator().next();
			default -> throw new MultipleAttributeNaturesException( backingMember.resolveAttributeName(), natures );
		};
	}
}
