/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLockableAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistentAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularAssociationAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularFetchModeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbStandardAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTransientImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AccessJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AttributeAccessorAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.boot.models.annotations.internal.MapsIdJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OnDeleteAnnotation;
import org.hibernate.boot.models.annotations.internal.OptimisticLockAnnotation;
import org.hibernate.boot.models.annotations.spi.AttributeMarker;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.FetchType;

/**
 * @author Steve Ebersole
 */
public class CommonAttributeProcessing {

	public static void applyAccess(
			AccessType accessType,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final AccessJpaAnnotation accessAnn = (AccessJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.ACCESS,
				xmlDocumentContext.getModelBuildingContext()
		);
		accessAnn.value( accessType );
	}

	public static void applyAttributeAccessor(
			JaxbPersistentAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final String attributeAccessor = jaxbAttribute.getAttributeAccessor();
		if ( attributeAccessor == null ) {
			return;
		}

		final AttributeAccessorAnnotation accessorAnn = (AttributeAccessorAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.ATTRIBUTE_ACCESSOR,
				xmlDocumentContext.getModelBuildingContext()
		);

		final ClassDetails strategyClassDetails = xmlDocumentContext
				.getModelBuildingContext()
				.getClassDetailsRegistry()
				.getClassDetails( attributeAccessor );
		accessorAnn.strategy( strategyClassDetails.toJavaClass() );
	}

	public static void applyOptionality(
			JaxbStandardAttribute jaxbAttribute,
			AttributeMarker.Optionalable attributeAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbAttribute.isOptional() == null ) {
			return;
		}

		attributeAnn.optional( jaxbAttribute.isOptional() );
	}

	public static void applyOptimisticLock(
			JaxbLockableAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final Boolean includeInOptimisticLock = jaxbAttribute.isOptimisticLock();

		if ( includeInOptimisticLock != null ) {
			final OptimisticLockAnnotation optLockAnn = (OptimisticLockAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.OPTIMISTIC_LOCK,
					xmlDocumentContext.getModelBuildingContext()
			);
			optLockAnn.excluded( !includeInOptimisticLock );
		}
	}

	public static void applyFetching(
			JaxbStandardAttribute jaxbAttribute,
			MutableMemberDetails memberDetails,
			AttributeMarker.Fetchable attributeAnn,
			XmlDocumentContext xmlDocumentContext) {
		final FetchType fetchType = jaxbAttribute.getFetch();
		if ( fetchType != null ) {
			attributeAnn.fetch( fetchType );
		}

		if ( jaxbAttribute instanceof JaxbSingularAssociationAttribute jaxbSingularAttribute ) {
			final JaxbSingularFetchModeImpl jaxbFetchMode = jaxbSingularAttribute.getFetchMode();
			applyFetchMode( memberDetails, jaxbFetchMode, xmlDocumentContext );
		}
		else if ( jaxbAttribute instanceof JaxbAnyMappingImpl jaxbAnyAttribute ) {
			final JaxbSingularFetchModeImpl jaxbFetchMode = jaxbAnyAttribute.getFetchMode();
			applyFetchMode( memberDetails, jaxbFetchMode, xmlDocumentContext );
		}
	}

	private static void applyFetchMode(
			MutableMemberDetails memberDetails,
			JaxbSingularFetchModeImpl jaxbFetchMode,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbFetchMode != null ) {
			final FetchMode fetchMode = FetchMode.valueOf( jaxbFetchMode.value() );
			final FetchAnnotation fetchAnn = (FetchAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.FETCH,
					xmlDocumentContext.getModelBuildingContext()
			);
			fetchAnn.value( fetchMode );
		}
	}

	public static void applyTransient(
			JaxbTransientImpl jaxbTransient,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
			jaxbTransient.getName(),
			classAccessType,
			declarer
		);
		memberDetails.applyAnnotationUsage( JpaAnnotations.TRANSIENT, xmlDocumentContext.getModelBuildingContext() );
	}

	public static void applyMappedBy(
			String mappedBy,
			AttributeMarker.Mappable mappable,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( mappedBy ) ) {
			mappable.mappedBy( mappedBy );
		}
	}

	public static void applyMapsId(
			String mapsId,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( mapsId ) ) {
			final MapsIdJpaAnnotation mapsIdUsage = (MapsIdJpaAnnotation) memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAPS_ID,
					xmlDocumentContext.getModelBuildingContext()
			);
			mapsIdUsage.value( mapsId );
		}

	}

	public static void applyOnDelete(
			OnDeleteAction action,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( action == null ) {
			return;
		}

		final OnDeleteAnnotation notFoundAnn = (OnDeleteAnnotation) memberDetails.applyAnnotationUsage(
				HibernateAnnotations.ON_DELETE,
				xmlDocumentContext.getModelBuildingContext()
		);
		notFoundAnn.action( action );
	}
}
