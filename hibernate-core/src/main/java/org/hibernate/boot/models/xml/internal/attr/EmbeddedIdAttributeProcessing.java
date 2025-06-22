/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.EmbeddedIdJpaAnnotation;
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
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class EmbeddedIdAttributeProcessing {

	public static MutableMemberDetails processEmbeddedIdAttribute(
			JaxbEmbeddedIdImpl jaxbEmbeddedId,
			MutableClassDetails classDetails,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbEmbeddedId.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbEmbeddedId.getName(),
				accessType,
				classDetails
		);

		final EmbeddedIdJpaAnnotation idAnn = (EmbeddedIdJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.EMBEDDED_ID,
				xmlDocumentContext.getModelBuildingContext()
		);

		applyAccess( accessType, memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbEmbeddedId, memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbEmbeddedId.getTarget() ) ) {
			final TargetXmlAnnotation targetAnn = (TargetXmlAnnotation) memberDetails.applyAnnotationUsage(
					XmlAnnotations.TARGET,
					xmlDocumentContext.getModelBuildingContext()
			);
			targetAnn.value( determineTargetName( jaxbEmbeddedId.getTarget(), xmlDocumentContext ) );
		}

		XmlAnnotationHelper.applyAttributeOverrides( jaxbEmbeddedId.getAttributeOverrides(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}
}
