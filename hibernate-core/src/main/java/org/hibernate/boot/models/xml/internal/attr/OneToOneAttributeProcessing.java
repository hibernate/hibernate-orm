/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.OneToOne;

import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.applyCascading;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.determineTargetName;
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

		TableProcessing.applyJoinTable( jaxbOneToOne.getJoinTable(), memberDetails, xmlDocumentContext );
		JoinColumnProcessing.applyJoinColumns( jaxbOneToOne.getJoinColumn(), memberDetails, xmlDocumentContext );
		JoinColumnProcessing.applyPrimaryKeyJoinColumns( jaxbOneToOne.getPrimaryKeyJoinColumn(), memberDetails, xmlDocumentContext );

		if ( jaxbOneToOne.isId() == Boolean.TRUE ) {
			memberDetails.applyAnnotationUsage(
					JpaAnnotations.ID,
					xmlDocumentContext.getModelBuildingContext()
			);
		}

		if ( StringHelper.isNotEmpty( jaxbOneToOne.getMapsId() ) ) {
			memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAPS_ID,
					(usage) -> usage.setAttributeValue( "value", jaxbOneToOne.getMapsId() ),
					xmlDocumentContext.getModelBuildingContext()
			);
		}

		return memberDetails;
	}

	private static MutableAnnotationUsage<OneToOne> applyOneToOne(
			MutableMemberDetails memberDetails,
			JaxbOneToOneImpl jaxbOneToOne,
			XmlDocumentContext xmlDocumentContext) {
		return memberDetails.applyAnnotationUsage(
				JpaAnnotations.ONE_TO_ONE,
				(usage) -> {
					if ( jaxbOneToOne.getFetch() != null ) {
						usage.setAttributeValue( "fetch", jaxbOneToOne.getFetch() );
					}
					if ( jaxbOneToOne.isOptional() != null ) {
						usage.setAttributeValue( "optional", jaxbOneToOne.isOptional() );
					}
					if ( StringHelper.isNotEmpty( jaxbOneToOne.getMappedBy() ) ) {
						usage.setAttributeValue( "mappedBy", jaxbOneToOne.getMappedBy() );
					}
					if ( jaxbOneToOne.isOrphanRemoval() != null ) {
						usage.setAttributeValue( "orphanRemoval", jaxbOneToOne.isOrphanRemoval() );
					}
				},
				xmlDocumentContext.getModelBuildingContext()
		);
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

		final MutableAnnotationUsage<Target> targetUsage = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.TARGET,
				xmlDocumentContext.getModelBuildingContext()
		);
		final String targetName = determineTargetName( targetEntityName, xmlDocumentContext );
		targetUsage.setAttributeValue( "value", targetName );
	}

}
