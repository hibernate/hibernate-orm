/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.AnnotationDescriptor;

import jakarta.persistence.AccessType;
import jakarta.persistence.OneToOne;

import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.applyCascading;
import static org.hibernate.boot.models.xml.internal.XmlProcessingHelper.getOrMakeAnnotation;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeBasics;
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

		final MutableAnnotationUsage<OneToOne> oneToOneAnn = applyOneToOne(
				memberDetails,
				jaxbOneToOne,
				xmlDocumentContext
		);

		applyAttributeBasics( jaxbOneToOne, memberDetails, oneToOneAnn, accessType, xmlDocumentContext );
		applyTarget( memberDetails, jaxbOneToOne, oneToOneAnn, xmlDocumentContext );
		applyCascading( jaxbOneToOne.getCascade(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	private static MutableAnnotationUsage<OneToOne> applyOneToOne(
			MutableMemberDetails memberDetails,
			JaxbOneToOneImpl jaxbOneToOne,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<OneToOne> oneToOneAnn = getOrMakeAnnotation( OneToOne.class, memberDetails, xmlDocumentContext );
		final AnnotationDescriptor<OneToOne> oneToOneDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( OneToOne.class );

		XmlAnnotationHelper.applyOr(
				jaxbOneToOne,
				JaxbOneToOneImpl::getFetch,
				"fetch",
				oneToOneAnn,
				oneToOneDescriptor
		);

		return oneToOneAnn;
	}

	@SuppressWarnings("unused")
	private static void applyTarget(
			MutableMemberDetails memberDetails,
			JaxbOneToOneImpl jaxbOneToOne,
			MutableAnnotationUsage<OneToOne> oneToOneAnn,
			XmlDocumentContext xmlDocumentContext) {
		final String targetEntityName = jaxbOneToOne.getTargetEntity();
		if ( StringHelper.isEmpty( targetEntityName ) ) {
			return;
		}

		final MutableAnnotationUsage<Target> targetAnn = XmlProcessingHelper.makeAnnotation( Target.class, memberDetails, xmlDocumentContext );
		targetAnn.setAttributeValue( "value", targetEntityName );
	}
}
