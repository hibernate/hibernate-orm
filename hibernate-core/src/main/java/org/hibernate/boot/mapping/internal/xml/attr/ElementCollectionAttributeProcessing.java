/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.xml.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TargetXmlAnnotation;
import org.hibernate.boot.mapping.internal.xml.SimpleTypeInterpretation;
import org.hibernate.boot.mapping.internal.xml.XmlAnnotationHelper;
import org.hibernate.boot.mapping.internal.xml.XmlProcessingHelper;
import org.hibernate.boot.mapping.internal.xml.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;

import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyFetching;
import static org.hibernate.boot.mapping.internal.xml.attr.CommonAttributeProcessing.applyOptimisticLock;
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

		final ModelsContext buildingContext = xmlDocumentContext.getModelsContext();
		final ElementCollectionJpaAnnotation elementCollectionUsage = (ElementCollectionJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.ELEMENT_COLLECTION,
				buildingContext
		);

		if ( jaxbElementCollection.getFetch() != null ) {
			elementCollectionUsage.fetch( jaxbElementCollection.getFetch() );
		}

		if ( jaxbElementCollection.isMutable() != null && !jaxbElementCollection.isMutable() ) {
			memberDetails.applyAnnotationUsage( HibernateAnnotations.IMMUTABLE, buildingContext );
		}

		applyElementCollectionElementType( jaxbElementCollection, elementCollectionUsage, memberDetails, xmlDocumentContext );

		// NOTE: it is important that this happens before the `CommonPluralAttributeProcessing#applyPluralAttributeStructure`
		// call below
		XmlAnnotationHelper.applyConverts(
				jaxbElementCollection.getConverts(),
				null,
				memberDetails,
				xmlDocumentContext
		);

		applyAccess( jaxbElementCollection.getAccess(), memberDetails, xmlDocumentContext );
		applyAttributeAccessor( jaxbElementCollection, memberDetails, xmlDocumentContext );
		applyFetching( jaxbElementCollection, memberDetails, elementCollectionUsage, xmlDocumentContext );
		applyOptimisticLock( jaxbElementCollection, memberDetails, xmlDocumentContext );

		CommonPluralAttributeProcessing.applyPluralAttributeStructure( jaxbElementCollection, memberDetails, xmlDocumentContext );

		applyCollectionTable( jaxbElementCollection.getCollectionTable(), memberDetails, xmlDocumentContext );


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// elements

		XmlAnnotationHelper.applyColumn( jaxbElementCollection.getColumn(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyEnumerated( jaxbElementCollection.getEnumerated(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyLob( jaxbElementCollection.getLob(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyNationalized( jaxbElementCollection.getNationalized(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyTemporal( jaxbElementCollection.getTemporal(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyBasicTypeComposition( jaxbElementCollection, memberDetails, xmlDocumentContext );


		XmlAnnotationHelper.applyAttributeOverrides( jaxbElementCollection, memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyAssociationOverrides( jaxbElementCollection.getAssociationOverrides(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	private static void applyElementCollectionElementType(
			JaxbElementCollectionImpl jaxbElementCollection,
			ElementCollectionJpaAnnotation elementCollectionUsage,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbElementCollection.getTargetClass() ) ) {
			final SimpleTypeInterpretation simpleTypeInterpretation = SimpleTypeInterpretation.interpret( jaxbElementCollection.getTargetClass() );
			if ( simpleTypeInterpretation != null ) {
				elementCollectionUsage.targetClass( simpleTypeInterpretation.getJavaType() );
				return;
			}
		}

		applyTarget( jaxbElementCollection, xmlDocumentContext, memberDetails );
	}

	private static void applyTarget(
			JaxbElementCollectionImpl jaxbElementCollection,
			XmlDocumentContext xmlDocumentContext,
			MutableMemberDetails memberDetails) {
		final String targetClass = jaxbElementCollection.getTargetClass();
		if ( StringHelper.isNotEmpty( targetClass ) ) {
			final TargetXmlAnnotation targetUsage = (TargetXmlAnnotation) memberDetails.applyAnnotationUsage(
					XmlAnnotations.TARGET,
					xmlDocumentContext.getModelsContext()
			);
			targetUsage.value( xmlDocumentContext.resolveTargetEntityName( targetClass ) );
		}
	}

	public static void applyCollectionTable(
			JaxbCollectionTableImpl jaxbCollectionTable,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( jaxbCollectionTable == null ) {
			return;
		}

		final CollectionTableJpaAnnotation collectionTableAnn = (CollectionTableJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.COLLECTION_TABLE,
				xmlDocumentContext.getModelsContext()
		);

		collectionTableAnn.apply( jaxbCollectionTable, xmlDocumentContext );
	}
}
