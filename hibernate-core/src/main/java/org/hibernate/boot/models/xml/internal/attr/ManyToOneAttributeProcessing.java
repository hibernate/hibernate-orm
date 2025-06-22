/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PropertyRefAnnotation;
import org.hibernate.boot.models.annotations.internal.TargetXmlAnnotation;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;

import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.applyNotFound;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyFetching;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptimisticLock;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptionality;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class ManyToOneAttributeProcessing {

	public static MutableMemberDetails processManyToOneAttribute(
			JaxbManyToOneImpl jaxbManyToOne,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbManyToOne.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbManyToOne.getName(),
				accessType,
				declarer
		);

		final ManyToOneJpaAnnotation manyToOneAnn = applyManyToOne(
				memberDetails,
				jaxbManyToOne,
				xmlDocumentContext
		);

		applyAccess( accessType, memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbManyToOne, memberDetails, xmlDocumentContext );
		applyFetching( jaxbManyToOne, memberDetails, manyToOneAnn, xmlDocumentContext );
		applyOptionality( jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		applyOptimisticLock( jaxbManyToOne, memberDetails, xmlDocumentContext );

		// todo (7.0) : cascades?

		TableProcessing.transformJoinTable( jaxbManyToOne.getJoinTable(), memberDetails, xmlDocumentContext );
		JoinColumnProcessing.applyJoinColumnsOrFormulas( jaxbManyToOne.getJoinColumnOrJoinFormula(), memberDetails, xmlDocumentContext );
		if ( jaxbManyToOne.getPropertyRef() != null ) {
			final PropertyRefAnnotation propertyRefUsage = (PropertyRefAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.PROPERTY_REF,
					xmlDocumentContext.getModelBuildingContext()
			);
			propertyRefUsage.value( jaxbManyToOne.getPropertyRef().getName() );
		}

		applyNotFound( jaxbManyToOne, memberDetails, xmlDocumentContext );
		applyOnDelete( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		applyTarget( memberDetails, jaxbManyToOne, manyToOneAnn, xmlDocumentContext );
		XmlAnnotationHelper.applyCascading( jaxbManyToOne.getCascade(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	private static ManyToOneJpaAnnotation applyManyToOne(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			XmlDocumentContext xmlDocumentContext) {
		final ManyToOneJpaAnnotation manyToOneUsage = (ManyToOneJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.MANY_TO_ONE,
				xmlDocumentContext.getModelBuildingContext()
		);

		if ( jaxbManyToOne.isId() == Boolean.TRUE ) {
			memberDetails.applyAnnotationUsage( JpaAnnotations.ID, xmlDocumentContext.getModelBuildingContext() );
		}

		CommonAttributeProcessing.applyMapsId( jaxbManyToOne.getMapsId(), memberDetails, xmlDocumentContext );

		return manyToOneUsage;
	}

	@SuppressWarnings("unused")
	private static void applyOnDelete(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			ManyToOneJpaAnnotation manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		CommonAttributeProcessing.applyOnDelete( jaxbManyToOne.getOnDelete(), memberDetails, xmlDocumentContext );
	}

	@SuppressWarnings("unused")
	private static void applyTarget(
			MutableMemberDetails memberDetails,
			JaxbManyToOneImpl jaxbManyToOne,
			ManyToOneJpaAnnotation manyToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		// todo (7.0) : we need a distinction here between hbm.xml target and orm.xml target-entity
		//		- for orm.xml target-entity we should apply the package name, if one
		//		- for hbm.xml target we should not since it could refer to a dynamic mapping
		final String targetEntityName = jaxbManyToOne.getTargetEntity();
		if ( StringHelper.isEmpty( targetEntityName ) ) {
			return;
		}

		final TargetXmlAnnotation targetAnn = (TargetXmlAnnotation) memberDetails.applyAnnotationUsage(
				XmlAnnotations.TARGET,
				xmlDocumentContext.getModelBuildingContext()
		);
		targetAnn.value( xmlDocumentContext.resolveClassName( targetEntityName ) );
	}
}
