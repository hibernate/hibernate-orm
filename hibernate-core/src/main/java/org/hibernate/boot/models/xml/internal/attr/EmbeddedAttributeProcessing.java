/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.TargetXmlAnnotation;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;

import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.determineTargetName;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptimisticLock;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class EmbeddedAttributeProcessing {
	public static MutableMemberDetails processEmbeddedAttribute(
			JaxbEmbeddedImpl jaxbEmbedded,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbEmbedded.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbEmbedded.getName(),
				accessType,
				declarer
		);

		memberDetails.applyAnnotationUsage(
				JpaAnnotations.EMBEDDED,
				xmlDocumentContext.getModelBuildingContext()
		);

		if ( StringHelper.isNotEmpty( jaxbEmbedded.getTarget() ) ) {
			final TargetXmlAnnotation targetAnn = (TargetXmlAnnotation) memberDetails.applyAnnotationUsage(
					XmlAnnotations.TARGET,
					xmlDocumentContext.getModelBuildingContext()
			);
			targetAnn.value( determineTargetName( jaxbEmbedded.getTarget(), xmlDocumentContext ) );
		}

		applyAccess( accessType, memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbEmbedded, memberDetails, xmlDocumentContext );
		applyOptimisticLock( jaxbEmbedded, memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyAttributeOverrides( jaxbEmbedded.getAttributeOverrides(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyAssociationOverrides( jaxbEmbedded.getAssociationOverrides(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}
}
