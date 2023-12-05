/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.xml.internal.attr;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyDiscriminatorValueMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingDiscriminatorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.models.categorize.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.categorize.xml.internal.db.ColumnProcessing;
import org.hibernate.boot.models.categorize.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.internal.MutableAnnotationUsage;
import org.hibernate.models.internal.MutableClassDetails;
import org.hibernate.models.internal.MutableMemberDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;

import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;

import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class AnyMappingAttributeProcessing {

	public static MutableMemberDetails processAnyMappingAttribute(
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbHbmAnyMapping.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbHbmAnyMapping.getName(),
				accessType,
				declarer
		);

		final MutableAnnotationUsage<Any> anyAnn = XmlProcessingHelper.makeAnnotation( Any.class, memberDetails, xmlDocumentContext );

		CommonAttributeProcessing.applyAttributeBasics( jaxbHbmAnyMapping, memberDetails, anyAnn, accessType, xmlDocumentContext );

		applyDiscriminator( memberDetails, jaxbHbmAnyMapping, anyAnn, xmlDocumentContext );
		applyKey( memberDetails, jaxbHbmAnyMapping, anyAnn, xmlDocumentContext );

		return memberDetails;
	}

	private static void applyDiscriminator(
			MutableMemberDetails memberDetails,
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			MutableAnnotationUsage<Any> anyAnn,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbAnyMappingDiscriminatorImpl jaxbDiscriminator = jaxbHbmAnyMapping.getDiscriminator();
		final MutableAnnotationUsage<AnyDiscriminator> anyDiscriminatorAnn = XmlProcessingHelper.makeAnnotation( AnyDiscriminator.class, memberDetails, xmlDocumentContext );

		if ( jaxbDiscriminator == null ) {
			return;
		}

		final DiscriminatorType discriminatorType = jaxbDiscriminator.getType();
		anyDiscriminatorAnn.setAttributeValue( "value", discriminatorType );

		final JaxbColumnImpl jaxbColumn = jaxbDiscriminator.getColumn();
		final MutableAnnotationUsage<Column> columnAnn = XmlProcessingHelper.makeAnnotation( Column.class, memberDetails, xmlDocumentContext );
		if ( jaxbColumn != null ) {
			ColumnProcessing.applyColumnDetails( jaxbColumn, memberDetails, columnAnn, xmlDocumentContext );
		}

		final List<JaxbAnyDiscriminatorValueMappingImpl> valueMappings = jaxbDiscriminator.getValueMappings();
		if ( CollectionHelper.isNotEmpty( valueMappings ) ) {
			final MutableAnnotationUsage<AnyDiscriminatorValues> valuesAnn = XmlProcessingHelper.makeAnnotation( AnyDiscriminatorValues.class, memberDetails, xmlDocumentContext );
			final List<MutableAnnotationUsage<AnyDiscriminatorValue>> valueList = CollectionHelper.arrayList( valueMappings.size() );
			final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry();
			valuesAnn.setAttributeValue( "value", valueList );
			valueMappings.forEach( (valueMapping) -> {
				final MutableAnnotationUsage<AnyDiscriminatorValue> valueAnn = XmlProcessingHelper.makeNestedAnnotation( AnyDiscriminatorValue.class, memberDetails, xmlDocumentContext );
				valueList.add( valueAnn );

				valueAnn.setAttributeValue( "discriminator", valueMapping.getDiscriminatorValue() );

				final String name = StringHelper.qualifyConditionally(
						xmlDocumentContext.getXmlDocument().getDefaults().getPackage(),
						valueMapping.getCorrespondingEntityName()
				);
				final ClassDetails entityClassDetails = classDetailsRegistry.resolveClassDetails( name );
				valueAnn.setAttributeValue( "entity", entityClassDetails );
			} );
		}
	}

	private static void applyKey(
			MutableMemberDetails memberDetails,
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			MutableAnnotationUsage<Any> anyAnn,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbAnyMappingKeyImpl jaxbKey = jaxbHbmAnyMapping.getKey();
		if ( StringHelper.isNotEmpty( jaxbKey.getType() ) ) {
			final MutableAnnotationUsage<AnyKeyType> keyTypeAnn = XmlProcessingHelper.makeAnnotation( AnyKeyType.class, memberDetails, xmlDocumentContext );
			keyTypeAnn.setAttributeValue( "value", jaxbKey.getType() );
		}

		if ( jaxbKey.getColumns().isEmpty() ) {
			XmlProcessingHelper.makeAnnotation( JoinColumn.class, memberDetails, xmlDocumentContext );
		}
		else {
			final MutableAnnotationUsage<JoinColumns> columnsAnn = XmlProcessingHelper.makeAnnotation( JoinColumns.class, memberDetails, xmlDocumentContext );
			final ArrayList<MutableAnnotationUsage<JoinColumn>> columnAnnList = CollectionHelper.arrayList( jaxbKey.getColumns().size() );
			columnsAnn.setAttributeValue( "value", columnAnnList );
			jaxbKey.getColumns().forEach( (jaxbColumn) -> {
				final MutableAnnotationUsage<JoinColumn> columnAnn = XmlProcessingHelper.makeNestedAnnotation( JoinColumn.class, memberDetails, xmlDocumentContext );
				columnAnnList.add( columnAnn );

				ColumnProcessing.applyColumnDetails( jaxbColumn, memberDetails, columnAnn, xmlDocumentContext );
			} );
		}
	}

}
