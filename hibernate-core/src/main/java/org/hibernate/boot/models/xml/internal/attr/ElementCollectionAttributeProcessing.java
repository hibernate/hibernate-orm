/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.internal.Target;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;

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

		final MutableAnnotationUsage<ElementCollection> elementCollectionUsage = memberDetails.applyAnnotationUsage(
				JpaAnnotations.ELEMENT_COLLECTION,
				xmlDocumentContext.getModelBuildingContext()
		);
		XmlProcessingHelper.applyAttributeIfSpecified(
				"fetch",
				jaxbElementCollection.getFetch(),
				elementCollectionUsage
		);

		final String targetClass = jaxbElementCollection.getTargetClass();
		if ( targetClass != null ) {
			final MutableAnnotationUsage<Target> targetUsage = memberDetails.applyAnnotationUsage(
					HibernateAnnotations.TARGET,
					xmlDocumentContext.getModelBuildingContext()
			);
			targetUsage.setAttributeValue( "value", xmlDocumentContext.resolveClassName( targetClass ) );
		}

		CommonAttributeProcessing.applyAttributeBasics( jaxbElementCollection, memberDetails, elementCollectionUsage, accessType, xmlDocumentContext );

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

		jaxbElementCollection.getConverts().forEach( (jaxbConvert) -> XmlAnnotationHelper.applyConvert(
				jaxbConvert,
				memberDetails,
				xmlDocumentContext
		) );

		XmlAnnotationHelper.applyAttributeOverrides( jaxbElementCollection, memberDetails, xmlDocumentContext );
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

		XmlAnnotationHelper.applyOr( jaxbCollectionTable, JaxbCollectionTableImpl::getName, "name", collectionTableAnn, collectionTableDescriptor );
		XmlAnnotationHelper.applyOrSchema(
				jaxbCollectionTable,
				collectionTableAnn,
				collectionTableDescriptor,
				xmlDocumentContext
		);

		XmlAnnotationHelper.applyOrCatalog(
				jaxbCollectionTable,
				collectionTableAnn,
				collectionTableDescriptor,
				xmlDocumentContext
		);
		XmlAnnotationHelper.applyOr( jaxbCollectionTable, JaxbCollectionTableImpl::getOptions, "options", collectionTableAnn, collectionTableDescriptor );

		collectionTableAnn.setAttributeValue( "joinColumns", JoinColumnProcessing.transformJoinColumnList(
				jaxbCollectionTable.getJoinColumns(),
				memberDetails,
				xmlDocumentContext
		) );

		if ( jaxbCollectionTable.getForeignKeys() != null ) {
			collectionTableAnn.setAttributeValue(
					"foreignKey",
					ForeignKeyProcessing.createNestedForeignKeyAnnotation( jaxbCollectionTable.getForeignKeys(), memberDetails, xmlDocumentContext )
			);
		}

		XmlAnnotationHelper.applyUniqueConstraints( jaxbCollectionTable.getUniqueConstraints(), memberDetails, collectionTableAnn, xmlDocumentContext );

		XmlAnnotationHelper.applyIndexes( jaxbCollectionTable.getIndexes(), memberDetails, collectionTableAnn, xmlDocumentContext );
	}
}
