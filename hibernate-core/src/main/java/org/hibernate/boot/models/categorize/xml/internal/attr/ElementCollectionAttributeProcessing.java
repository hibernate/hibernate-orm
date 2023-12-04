/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal.attr;

import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.AnnotationDescriptor;

import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;

import static org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper.applyIndexes;
import static org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper.applyOr;
import static org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper.applyUniqueConstraints;
import static org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper.createForeignKeyAnnotation;
import static org.hibernate.boot.models.categorize.xml.internal.XmlAnnotationHelper.createJoinColumns;
import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class ElementCollectionAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processElementCollectionAttribute(
			JaxbElementCollectionImpl jaxbElementCollection,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbElementCollection.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbElementCollection.getName(),
				accessType,
				declarer
		);

		final MutableAnnotationUsage<ElementCollection> elementCollectionAnn = XmlProcessingHelper.getOrMakeAnnotation(
				ElementCollection.class,
				memberDetails,
				xmlDocumentContext
		);
		XmlProcessingHelper.setIf( jaxbElementCollection.getFetch(), "fetch", elementCollectionAnn );
		if ( StringHelper.isNotEmpty( jaxbElementCollection.getTargetClass() ) ) {
			elementCollectionAnn.setAttributeValue(
					"targetClass",
					XmlAnnotationHelper.resolveJavaType(
							jaxbElementCollection.getTargetClass(),
							xmlDocumentContext.getModelBuildingContext()
					)
			);
		}

		CommonAttributeProcessing.applyAttributeBasics( jaxbElementCollection, memberDetails, elementCollectionAnn, accessType, xmlDocumentContext );

		CommonPluralAttributeProcessing.applyPluralAttributeStructure( jaxbElementCollection, memberDetails, xmlDocumentContext );

		applyCollectionTable( jaxbElementCollection.getCollectionTable(), memberDetails, xmlDocumentContext );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// elements

		XmlAnnotationHelper.applyEnumerated( jaxbElementCollection.getEnumerated(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyLob( jaxbElementCollection.getLob(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyNationalized( jaxbElementCollection.getNationalized(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyTemporal( jaxbElementCollection.getTemporal(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyBasicTypeComposition( jaxbElementCollection, memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbElementCollection.getTargetClass() ) ) {
			final MutableAnnotationUsage<Target> targetAnn = XmlProcessingHelper.getOrMakeAnnotation( Target.class, memberDetails, xmlDocumentContext );
			targetAnn.setAttributeValue( "value", jaxbElementCollection.getTargetClass() );
		}

		jaxbElementCollection.getConverts().forEach( (jaxbConvert) -> {
			XmlAnnotationHelper.applyConvert( jaxbConvert, memberDetails, xmlDocumentContext );
		} );

		XmlAnnotationHelper.applyAttributeOverrides( jaxbElementCollection.getAttributeOverrides(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyAssociationOverrides( jaxbElementCollection.getAssociationOverrides(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	public static void applyCollectionTable(
			JaxbCollectionTableImpl jaxbCollectionTable,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCollectionTable == null ) {
			return;
		}

		final MutableAnnotationUsage<CollectionTable> collectionTableAnn = XmlProcessingHelper.getOrMakeAnnotation(
				CollectionTable.class,
				memberDetails,
				xmlDocumentContext
		);
		final AnnotationDescriptor<CollectionTable> collectionTableDescriptor = xmlDocumentContext.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( CollectionTable.class );

		applyOr( jaxbCollectionTable, JaxbCollectionTableImpl::getName, "name", collectionTableAnn, collectionTableDescriptor );
		applyOr( jaxbCollectionTable, JaxbCollectionTableImpl::getSchema, "schema", collectionTableAnn, collectionTableDescriptor );
		applyOr( jaxbCollectionTable, JaxbCollectionTableImpl::getOptions, "options", collectionTableAnn, collectionTableDescriptor );

		collectionTableAnn.setAttributeValue( "joinColumns", createJoinColumns( jaxbCollectionTable.getJoinColumns(), memberDetails, xmlDocumentContext ) );

		if ( jaxbCollectionTable.getForeignKeys() != null ) {
			collectionTableAnn.setAttributeValue(
					"foreignKey",
					createForeignKeyAnnotation( jaxbCollectionTable.getForeignKeys(), memberDetails, xmlDocumentContext )
			);
		}

		applyUniqueConstraints( jaxbCollectionTable.getUniqueConstraints(), memberDetails, collectionTableAnn, xmlDocumentContext );

		applyIndexes( jaxbCollectionTable.getIndexes(), memberDetails, collectionTableAnn, xmlDocumentContext );
	}
}
