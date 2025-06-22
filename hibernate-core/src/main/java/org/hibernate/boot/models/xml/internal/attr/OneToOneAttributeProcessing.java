/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.OneToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PropertyRefAnnotation;
import org.hibernate.boot.models.annotations.internal.TargetXmlAnnotation;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;

import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.applyCascading;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyFetching;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptimisticLock;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptionality;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class OneToOneAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processOneToOneAttribute(
			JaxbOneToOneImpl jaxbOneToOne,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbOneToOne.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbOneToOne.getName(),
				accessType,
				declarer
		);

		final OneToOneJpaAnnotation oneToOneAnn = applyOneToOne(
				memberDetails,
				accessType,
				jaxbOneToOne,
				xmlDocumentContext
		);

		applyMappedBy( memberDetails, jaxbOneToOne, oneToOneAnn, xmlDocumentContext );
		applyTarget( memberDetails, jaxbOneToOne, oneToOneAnn, xmlDocumentContext );
		applyCascading( jaxbOneToOne.getCascade(), memberDetails, xmlDocumentContext );

		TableProcessing.transformJoinTable( jaxbOneToOne.getJoinTable(), memberDetails, xmlDocumentContext );
		JoinColumnProcessing.applyJoinColumnsOrFormulas( jaxbOneToOne.getJoinColumnOrJoinFormula(), memberDetails, xmlDocumentContext );
		JoinColumnProcessing.applyPrimaryKeyJoinColumns( jaxbOneToOne.getPrimaryKeyJoinColumn(), memberDetails, xmlDocumentContext );

		if ( jaxbOneToOne.getPropertyRef() != null ) {
			final PropertyRefAnnotation propertyRefUsage = (PropertyRefAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.PROPERTY_REF,
					xmlDocumentContext.getModelBuildingContext()
			);
			propertyRefUsage.value( jaxbOneToOne.getPropertyRef().getName() );
		}

		if ( jaxbOneToOne.isId() == Boolean.TRUE ) {
			memberDetails.applyAnnotationUsage(
					JpaAnnotations.ID,
					xmlDocumentContext.getModelBuildingContext()
			);
		}

		CommonAttributeProcessing.applyMapsId( jaxbOneToOne.getMapsId(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	private static OneToOneJpaAnnotation applyOneToOne(
			MutableMemberDetails memberDetails,
			AccessType accessType,
			JaxbOneToOneImpl jaxbOneToOne,
			XmlDocumentContext xmlDocumentContext) {
		final OneToOneJpaAnnotation annotation = (OneToOneJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.ONE_TO_ONE,
				xmlDocumentContext.getModelBuildingContext()
		);

		applyAccess( accessType, memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbOneToOne, memberDetails, xmlDocumentContext );
		applyFetching( jaxbOneToOne, memberDetails, annotation, xmlDocumentContext );
		applyOptionality( jaxbOneToOne, annotation, xmlDocumentContext );
		applyOptimisticLock( jaxbOneToOne, memberDetails, xmlDocumentContext );

		if ( jaxbOneToOne.isOrphanRemoval() != null ) {
			annotation.orphanRemoval( jaxbOneToOne.isOrphanRemoval() );
		}

		return annotation;
	}

	private static void applyMappedBy(
			MutableMemberDetails memberDetails,
			JaxbOneToOneImpl jaxbOneToOne,
			OneToOneJpaAnnotation oneToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbOneToOne.getMappedBy() ) ) {
			oneToOneAnn.mappedBy( jaxbOneToOne.getMappedBy() );
		}
	}

	private static void applyTarget(
			MutableMemberDetails memberDetails,
			JaxbOneToOneImpl jaxbOneToOne,
			OneToOneJpaAnnotation oneToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		// todo (7.0) : we need a distinction here between hbm.xml target and orm.xml target-entity
		//		- for orm.xml target-entity we should apply the package name, if one
		//		- for hbm.xml target we should not since it could refer to a dynamic mapping
		final String targetEntityName = jaxbOneToOne.getTargetEntity();
		if ( StringHelper.isEmpty( targetEntityName ) ) {
			return;
		}

		final TargetXmlAnnotation annotation = (TargetXmlAnnotation) memberDetails.applyAnnotationUsage(
				XmlAnnotations.TARGET,
				xmlDocumentContext.getModelBuildingContext()
		);
		annotation.value( xmlDocumentContext.resolveClassName( targetEntityName ) );
	}

}
