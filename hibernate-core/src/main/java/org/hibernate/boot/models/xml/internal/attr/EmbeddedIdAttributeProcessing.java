/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.EmbeddedId;

import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.determineTargetName;
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
		final MutableMemberDetails memberDetails = XmlProcessingHelper.findAttributeMember(
				jaxbEmbeddedId.getName(),
				accessType,
				classDetails
		);

		final MutableAnnotationUsage<EmbeddedId> idAnn = memberDetails.applyAnnotationUsage(
				JpaAnnotations.EMBEDDED_ID,
				xmlDocumentContext.getModelBuildingContext()
		);
		CommonAttributeProcessing.applyAttributeBasics( jaxbEmbeddedId, memberDetails, idAnn, accessType, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbEmbeddedId.getTarget() ) ) {
			final MutableAnnotationUsage<Target> targetAnn = memberDetails.applyAnnotationUsage(
					HibernateAnnotations.TARGET,
					xmlDocumentContext.getModelBuildingContext()
			);
			targetAnn.setAttributeValue( "value", determineTargetName( jaxbEmbeddedId.getTarget(), xmlDocumentContext ) );
		}

		XmlAnnotationHelper.applyAttributeOverrides( jaxbEmbeddedId.getAttributeOverrides(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}
}
