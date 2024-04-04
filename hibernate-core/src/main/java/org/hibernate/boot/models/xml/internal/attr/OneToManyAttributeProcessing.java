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
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.TableProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.AnnotationDescriptor;
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

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// join-table

		TableProcessing.applyJoinTable( jaxbOneToMany.getJoinTable(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applySqlJoinTableRestriction( jaxbOneToMany.getSqlJoinTableRestriction(), memberDetails, xmlDocumentContext );

		jaxbOneToMany.getJoinTableFilters().forEach( (jaxbFilter) -> {
			XmlAnnotationHelper.applyJoinTableFilter( jaxbFilter, memberDetails, xmlDocumentContext );
		} );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// other properties

		jaxbOneToMany.getJoinColumn().forEach( jaxbJoinColumn -> {
			XmlAnnotationHelper.applyJoinColumn( jaxbJoinColumn, memberDetails, xmlDocumentContext );
		} );

		if ( jaxbOneToMany.getOnDelete() != null ) {
			XmlProcessingHelper.getOrMakeAnnotation( OnDelete.class, memberDetails, xmlDocumentContext ).setAttributeValue( "action", jaxbOneToMany.getOnDelete() );
		}

		if ( jaxbOneToMany.getNotFound() != null ) {
			if ( jaxbOneToMany.getNotFound() != NotFoundAction.EXCEPTION ) {
				XmlProcessingHelper.getOrMakeAnnotation( NotFound.class, memberDetails, xmlDocumentContext ).setAttributeValue( "action", jaxbOneToMany.getNotFound() );
			}
		}

		return memberDetails;
	}

	private static MutableAnnotationUsage<OneToMany> applyOneToMany(
			JaxbOneToManyImpl jaxbOneToMany,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<OneToMany> oneToManyAnn = XmlProcessingHelper.getOrMakeAnnotation( OneToMany.class, memberDetails, xmlDocumentContext );
		final AnnotationDescriptor<OneToMany> oneToManyDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( OneToMany.class );

		XmlAnnotationHelper.applyOr( jaxbOneToMany, JaxbOneToManyImpl::getFetch, "fetch", oneToManyAnn, oneToManyDescriptor );
		XmlAnnotationHelper.applyOr( jaxbOneToMany, JaxbOneToManyImpl::getMappedBy, "mappedBy", oneToManyAnn, oneToManyDescriptor );
		XmlAnnotationHelper.applyOr( jaxbOneToMany, JaxbOneToManyImpl::isOrphanRemoval, "orphanRemoval", oneToManyAnn, oneToManyDescriptor );

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
