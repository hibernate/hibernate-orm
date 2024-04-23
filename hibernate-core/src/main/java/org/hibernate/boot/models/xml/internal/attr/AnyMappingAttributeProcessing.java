/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.boot.internal.AnyKeyType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyDiscriminatorValueMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingDiscriminatorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.ColumnProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

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

		final MutableAnnotationUsage<Any> anyAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.ANY,
				xmlDocumentContext.getModelBuildingContext()
		);

		CommonAttributeProcessing.applyAttributeBasics( jaxbHbmAnyMapping, memberDetails, anyAnn, accessType, xmlDocumentContext );

		applyDiscriminator( memberDetails, jaxbHbmAnyMapping, anyAnn, xmlDocumentContext );
		applyKey( memberDetails, jaxbHbmAnyMapping, anyAnn, xmlDocumentContext );
		XmlAnnotationHelper.applyCascading( jaxbHbmAnyMapping.getCascade(), memberDetails, xmlDocumentContext );

		return memberDetails;
	}

	private static void applyDiscriminator(
			MutableMemberDetails memberDetails,
			JaxbAnyMappingImpl jaxbHbmAnyMapping,
			MutableAnnotationUsage<Any> anyAnn,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbAnyMappingDiscriminatorImpl jaxbDiscriminator = jaxbHbmAnyMapping.getDiscriminator();
		final MutableAnnotationUsage<AnyDiscriminator> anyDiscriminatorAnn = memberDetails.applyAnnotationUsage(
				HibernateAnnotations.ANY_DISCRIMINATOR,
				xmlDocumentContext.getModelBuildingContext()
		);

		if ( jaxbDiscriminator == null ) {
			return;
		}

		final DiscriminatorType discriminatorType = jaxbDiscriminator.getType();
		XmlProcessingHelper.applyAttributeIfSpecified( "value", discriminatorType, anyDiscriminatorAnn );

		final JaxbColumnImpl jaxbColumn = jaxbDiscriminator.getColumn();
		final MutableAnnotationUsage<Column> columnAnn = memberDetails.applyAnnotationUsage(
				JpaAnnotations.COLUMN,
				xmlDocumentContext.getModelBuildingContext()
		);
		if ( jaxbColumn != null ) {
			ColumnProcessing.applyColumnDetails( jaxbColumn, memberDetails, columnAnn, xmlDocumentContext );
		}

		final ClassDetailsRegistry classDetailsRegistry = xmlDocumentContext.getModelBuildingContext().getClassDetailsRegistry();
		final List<JaxbAnyDiscriminatorValueMappingImpl> valueMappings = jaxbDiscriminator.getValueMappings();
		if ( CollectionHelper.isNotEmpty( valueMappings ) ) {
			final MutableAnnotationUsage<AnyDiscriminatorValues> discriminatorValuesUsage = memberDetails.applyAnnotationUsage(
					HibernateAnnotations.ANY_DISCRIMINATOR_VALUES,
					xmlDocumentContext.getModelBuildingContext()
			);
			final List<MutableAnnotationUsage<AnyDiscriminatorValue>> valueList = CollectionHelper.arrayList( valueMappings.size() );
			discriminatorValuesUsage.setAttributeValue( "value", valueList );

			valueMappings.forEach( (valueMapping) -> {
				final MutableAnnotationUsage<AnyDiscriminatorValue> discriminatorValueUsage = HibernateAnnotations.ANY_DISCRIMINATOR_VALUE.createUsage(
						null,
						xmlDocumentContext.getModelBuildingContext()
				);
				valueList.add( discriminatorValueUsage );

				discriminatorValueUsage.setAttributeValue( "discriminator", valueMapping.getDiscriminatorValue() );

				final String name = StringHelper.qualifyConditionally(
						xmlDocumentContext.getXmlDocument().getDefaults().getPackage(),
						valueMapping.getCorrespondingEntityName()
				);
				final ClassDetails entityClassDetails = classDetailsRegistry.resolveClassDetails( name );
				discriminatorValueUsage.setAttributeValue( "entity", entityClassDetails );
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
			final MutableAnnotationUsage<AnyKeyType> keyTypeUsage = memberDetails.applyAnnotationUsage(
					HibernateAnnotations.ANY_KEY_TYPE,
					xmlDocumentContext.getModelBuildingContext()
			);
			keyTypeUsage.setAttributeValue( "value", jaxbKey.getType() );
		}

		if ( jaxbKey.getColumns().isEmpty() ) {
			memberDetails.applyAnnotationUsage( JpaAnnotations.JOIN_COLUMN, xmlDocumentContext.getModelBuildingContext() );
		}
		else {
			final MutableAnnotationUsage<JoinColumns> joinColumnsUsage = memberDetails.applyAnnotationUsage(
					JpaAnnotations.JOIN_COLUMNS,
					xmlDocumentContext.getModelBuildingContext()
			);
			final ArrayList<MutableAnnotationUsage<JoinColumn>> columnAnnList = CollectionHelper.arrayList( jaxbKey.getColumns().size() );
			joinColumnsUsage.setAttributeValue( "value", columnAnnList );
			jaxbKey.getColumns().forEach( (jaxbColumn) -> {
				final MutableAnnotationUsage<JoinColumn> joinColumnUsage = JpaAnnotations.JOIN_COLUMN.createUsage(
						null,
						xmlDocumentContext.getModelBuildingContext()
				);
				columnAnnList.add( joinColumnUsage );

				ColumnProcessing.applyColumnDetails( jaxbColumn, memberDetails, joinColumnUsage, xmlDocumentContext );
			} );
		}
	}

}
