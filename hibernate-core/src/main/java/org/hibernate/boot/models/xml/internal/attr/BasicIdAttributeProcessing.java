/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.BasicJpaAnnotation;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.FetchType;

import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class BasicIdAttributeProcessing {

	public static MutableMemberDetails processBasicIdAttribute(
			JaxbIdImpl jaxbId,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbId.getAccess(), classAccessType );
		final String idAttributeName = StringHelper.isNotEmpty( jaxbId.getName() )
				? jaxbId.getName()
				: "id";

		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				idAttributeName,
				accessType,
				declarer
		);

		memberDetails.applyAnnotationUsage( JpaAnnotations.ID, xmlDocumentContext.getModelBuildingContext() );
		final BasicJpaAnnotation basicAnn = (BasicJpaAnnotation) memberDetails.applyAnnotationUsage( JpaAnnotations.BASIC, xmlDocumentContext.getModelBuildingContext() );
		basicAnn.fetch( FetchType.EAGER );
		basicAnn.optional( false );

		applyAccess( accessType, memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbId, memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyColumn( jaxbId.getColumn(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyBasicTypeComposition( jaxbId, memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyTemporal( jaxbId.getTemporal(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyGeneratedValue( jaxbId.getGeneratedValue(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applySequenceGenerator( jaxbId.getSequenceGenerator(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyTableGenerator( jaxbId.getTableGenerator(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyUuidGenerator( jaxbId.getUuidGenerator(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyGenericGenerator( jaxbId.getGenericGenerator(), memberDetails, xmlDocumentContext );

		// todo : unsaved-value?
		// todo : ...

		return memberDetails;
	}
}
