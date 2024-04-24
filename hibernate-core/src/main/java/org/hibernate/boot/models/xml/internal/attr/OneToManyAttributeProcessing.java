/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.OnDelete;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.OneToMany;

import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class OneToManyAttributeProcessing {
	public static MutableMemberDetails processOneToManyAttribute(
			JaxbOneToManyImpl jaxbOneToMany,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbOneToMany.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbOneToMany.getName(),
				accessType,
				declarer
		);

		final MutableAnnotationUsage<OneToMany> oneToManyAnn = applyOneToMany(
				jaxbOneToMany,
				memberDetails,
				xmlDocumentContext
		);

		applyTargetEntity( jaxbOneToMany, oneToManyAnn, xmlDocumentContext );

		CommonAttributeProcessing.applyAttributeBasics( jaxbOneToMany, memberDetails, oneToManyAnn, accessType, xmlDocumentContext );
		CommonPluralAttributeProcessing.applyPluralAttributeStructure( jaxbOneToMany, memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyCascading( jaxbOneToMany.getCascade(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyAttributeOverrides(
				jaxbOneToMany.getMapKeyAttributeOverrides(),
				memberDetails,
				"key",
				xmlDocumentContext
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// join-table

		TableProcessing.transformJoinTable( jaxbOneToMany.getJoinTable(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applySqlJoinTableRestriction( jaxbOneToMany.getSqlJoinTableRestriction(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyJoinTableFilters( jaxbOneToMany.getJoinTableFilters(), memberDetails, xmlDocumentContext );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// other properties

		JoinColumnProcessing.applyJoinColumns( jaxbOneToMany.getJoinColumn(), memberDetails, xmlDocumentContext );

		if ( jaxbOneToMany.getOnDelete() != null ) {
			final MutableAnnotationUsage<OnDelete> onDeleteAnn = memberDetails.applyAnnotationUsage(
					HibernateAnnotations.ON_DELETE,
					xmlDocumentContext.getModelBuildingContext()
			);
			onDeleteAnn.setAttributeValue( "action", jaxbOneToMany.getOnDelete() );
		}

		if ( jaxbOneToMany.getNotFound() != null ) {
			if ( jaxbOneToMany.getNotFound() != NotFoundAction.EXCEPTION ) {
				final MutableAnnotationUsage<NotFound> notFoundAnn = memberDetails.applyAnnotationUsage(
						HibernateAnnotations.NOT_FOUND,
						xmlDocumentContext.getModelBuildingContext()
				);
				notFoundAnn.setAttributeValue( "action", jaxbOneToMany.getNotFound() );
			}
		}

		return memberDetails;
	}

	private static MutableAnnotationUsage<OneToMany> applyOneToMany(
			JaxbOneToManyImpl jaxbOneToMany,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<OneToMany> oneToManyAnn = memberDetails.applyAnnotationUsage(
				JpaAnnotations.ONE_TO_MANY,
				xmlDocumentContext.getModelBuildingContext()
		);

		if ( jaxbOneToMany != null ) {
			XmlAnnotationHelper.applyOptionalAttribute( oneToManyAnn, "fetch", jaxbOneToMany.getFetch() );
			XmlAnnotationHelper.applyOptionalAttribute( oneToManyAnn, "mappedBy", jaxbOneToMany.getMappedBy() );
			XmlAnnotationHelper.applyOptionalAttribute( oneToManyAnn, "orphanRemoval", jaxbOneToMany.isOrphanRemoval() );
		}

		return oneToManyAnn;
	}

	private static void applyTargetEntity(
			JaxbOneToManyImpl jaxbOneToMany,
			MutableAnnotationUsage<OneToMany> oneToManyAnn,
			XmlDocumentContext xmlDocumentContext) {
		final String targetEntity = jaxbOneToMany.getTargetEntity();
		if ( StringHelper.isNotEmpty( targetEntity ) ) {
			oneToManyAnn.setAttributeValue(
					"targetEntity",
					xmlDocumentContext.resolveJavaType( targetEntity )
			);
		}
	}
}
