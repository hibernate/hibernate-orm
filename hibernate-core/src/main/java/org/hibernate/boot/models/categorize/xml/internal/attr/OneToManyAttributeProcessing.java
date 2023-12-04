/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal.attr;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.OnDelete;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.AnnotationDescriptor;

import jakarta.persistence.AccessType;
import jakarta.persistence.OneToMany;

import static org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper.applyOr;
import static org.hibernate.boot.models.categorize.xml.internal.XmlProcessingHelper.getOrMakeAnnotation;
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

		final MutableAnnotationUsage<OneToMany> oneToManyAnn = applyManyToOne(
				jaxbOneToMany,
				memberDetails,
				xmlDocumentContext
		);

		applyTargetEntity( jaxbOneToMany, oneToManyAnn, xmlDocumentContext );

		XmlAnnotationHelper.applyCascading( jaxbOneToMany.getCascade(), memberDetails, xmlDocumentContext );

		CommonAttributeProcessing.applyAttributeBasics( jaxbOneToMany, memberDetails, oneToManyAnn, accessType, xmlDocumentContext );

		CommonPluralAttributeProcessing.applyPluralAttributeStructure( jaxbOneToMany, memberDetails, xmlDocumentContext );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// join-table

		XmlAnnotationHelper.applyJoinTable( jaxbOneToMany.getJoinTable(), memberDetails, xmlDocumentContext );

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
			getOrMakeAnnotation( OnDelete.class, memberDetails, xmlDocumentContext ).setAttributeValue( "action", jaxbOneToMany.getOnDelete() );
		}

		if ( jaxbOneToMany.getNotFound() != null ) {
			getOrMakeAnnotation( NotFound.class, memberDetails, xmlDocumentContext ).setAttributeValue( "action", jaxbOneToMany.getNotFound() );
		}

		return memberDetails;
	}

	private static MutableAnnotationUsage<OneToMany> applyManyToOne(
			JaxbOneToManyImpl jaxbOneToMany,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<OneToMany> oneToManyAnn = getOrMakeAnnotation( OneToMany.class, memberDetails, xmlDocumentContext );
		final AnnotationDescriptor<OneToMany> oneToManyDescriptor = xmlDocumentContext
				.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( OneToMany.class );

		applyOr( jaxbOneToMany, JaxbOneToManyImpl::getFetch, "fetch", oneToManyAnn, oneToManyDescriptor );
		applyOr( jaxbOneToMany, JaxbOneToManyImpl::getMappedBy, "mappedBy", oneToManyAnn, oneToManyDescriptor );
		applyOr( jaxbOneToMany, JaxbOneToManyImpl::isOrphanRemoval, "orphanRemoval", oneToManyAnn, oneToManyDescriptor );

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
					xmlDocumentContext.getModelBuildingContext()
							.getClassDetailsRegistry()
							.resolveClassDetails( targetEntity )
			);
		}
	}
}
