/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.XmlAnnotations;
import org.hibernate.boot.models.annotations.internal.CollectionTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ElementCollectionJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TargetXmlAnnotation;
import org.hibernate.boot.models.xml.internal.SimpleTypeInterpretation;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;

import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAccess;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyAttributeAccessor;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyFetching;
import static org.hibernate.boot.models.xml.internal.attr.CommonAttributeProcessing.applyOptimisticLock;
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

		final ElementCollectionJpaAnnotation elementCollectionUsage = (ElementCollectionJpaAnnotation) memberDetails.applyAnnotationUsage(
				JpaAnnotations.ELEMENT_COLLECTION,
				xmlDocumentContext.getModelBuildingContext()
		);

		if ( jaxbElementCollection.getFetch() != null ) {
			elementCollectionUsage.fetch( jaxbElementCollection.getFetch() );
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

		applyAccess( accessType, memberDetails, xmlDocumentContext );
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
		// todo (7.0) : we need a distinction here between hbm.xml target and orm.xml target-entity
		//		- for orm.xml target-entity we should apply the package name, if one
		//		- for hbm.xml target we should not since it could refer to a dynamic mapping
		final String targetClass = jaxbElementCollection.getTargetClass();
		if ( StringHelper.isNotEmpty( targetClass ) ) {
			final TargetXmlAnnotation targetUsage = (TargetXmlAnnotation) memberDetails.applyAnnotationUsage(
					XmlAnnotations.TARGET,
					xmlDocumentContext.getModelBuildingContext()
			);
			targetUsage.value( xmlDocumentContext.resolveClassName( targetClass ) );
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
				xmlDocumentContext.getModelBuildingContext()
		);

		collectionTableAnn.apply( jaxbCollectionTable, xmlDocumentContext );
	}
}
