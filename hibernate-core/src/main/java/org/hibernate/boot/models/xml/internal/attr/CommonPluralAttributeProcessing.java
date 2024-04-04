/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.annotations.Bag;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLDeleteAll;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOrderColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.internal.db.ColumnProcessing;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableMemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;

import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.applyOr;

/**
 * @author Marco Belladelli
 */
public class CommonPluralAttributeProcessing {
	public static void applyPluralAttributeStructure(
			JaxbPluralAttribute jaxbPluralAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final SourceModelBuildingContext buildingContext = xmlDocumentContext.getModelBuildingContext();
		final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();

		if ( jaxbPluralAttribute.getFetchMode() != null ) {
			final MutableAnnotationUsage<Fetch> fetchAnn = XmlProcessingHelper.getOrMakeAnnotation( Fetch.class, memberDetails, xmlDocumentContext );
			fetchAnn.setAttributeValue( "value", jaxbPluralAttribute.getFetchMode() );
		}

		if ( jaxbPluralAttribute.getClassification() != null ) {
			final MutableAnnotationUsage<CollectionClassification> collectionClassificationAnn = XmlProcessingHelper.getOrMakeAnnotation(
					CollectionClassification.class,
					memberDetails,
					xmlDocumentContext
			);
			collectionClassificationAnn.setAttributeValue( "value", jaxbPluralAttribute.getClassification() );
			if ( jaxbPluralAttribute.getClassification() == LimitedCollectionClassification.BAG ) {
				XmlProcessingHelper.getOrMakeAnnotation( Bag.class, memberDetails, xmlDocumentContext );
			}
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// collection-structure

		XmlAnnotationHelper.applyCollectionUserType( jaxbPluralAttribute.getCollectionType(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyCollectionId( jaxbPluralAttribute.getCollectionId(), memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbPluralAttribute.getOrderBy() ) ) {
			final MutableAnnotationUsage<OrderBy> orderByAnn = XmlProcessingHelper.getOrMakeAnnotation(
					OrderBy.class,
					memberDetails,
					xmlDocumentContext
			);
			orderByAnn.setAttributeValue( "value", jaxbPluralAttribute.getOrderBy() );
		}

		applyOrderColumn( jaxbPluralAttribute, memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbPluralAttribute.getSort() ) ) {
			final MutableAnnotationUsage<SortComparator> sortAnn = XmlProcessingHelper.getOrMakeAnnotation(
					SortComparator.class,
					memberDetails,
					xmlDocumentContext
			);
			final ClassDetails comparatorClassDetails = classDetailsRegistry.resolveClassDetails( jaxbPluralAttribute.getSort() );
			sortAnn.setAttributeValue( "value", comparatorClassDetails );
		}

		if ( jaxbPluralAttribute.getSortNatural() != null ) {
			XmlProcessingHelper.getOrMakeAnnotation( SortNatural.class, memberDetails, xmlDocumentContext );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// map-key

		if ( jaxbPluralAttribute.getMapKey() != null ) {
			final MutableAnnotationUsage<MapKey> mapKeyAnn = XmlProcessingHelper.getOrMakeAnnotation( MapKey.class, memberDetails, xmlDocumentContext );
			applyOr(
					jaxbPluralAttribute.getMapKey(),
					JaxbMapKeyImpl::getName,
					"name",
					mapKeyAnn,
					buildingContext.getAnnotationDescriptorRegistry().getDescriptor( MapKey.class )
			);
		}

		if ( jaxbPluralAttribute.getMapKeyClass() != null ) {
			final String className = xmlDocumentContext.resolveClassName( jaxbPluralAttribute.getMapKeyClass().getClazz() );
			final ClassDetails mapKeyClass = classDetailsRegistry.resolveClassDetails( className );
			XmlProcessingHelper.getOrMakeAnnotation( MapKeyClass.class, memberDetails, xmlDocumentContext ).setAttributeValue( "value", mapKeyClass );
		}

		if ( jaxbPluralAttribute.getMapKeyTemporal() != null ) {
			XmlProcessingHelper.getOrMakeAnnotation( MapKeyTemporal.class, memberDetails, xmlDocumentContext ).setAttributeValue(
					"value",
					jaxbPluralAttribute.getMapKeyTemporal()
			);
		}

		if ( jaxbPluralAttribute.getMapKeyEnumerated() != null ) {
			XmlProcessingHelper.getOrMakeAnnotation( MapKeyEnumerated.class, memberDetails, xmlDocumentContext ).setAttributeValue(
					"value",
					jaxbPluralAttribute.getMapKeyEnumerated()
			);
		}

		jaxbPluralAttribute.getMapKeyConverts().forEach( (jaxbConvert) -> {
			XmlAnnotationHelper.applyConvert( jaxbConvert, memberDetails, "key", xmlDocumentContext );
		} );

		ColumnProcessing.applyMapKeyColumn( jaxbPluralAttribute.getMapKeyColumn(), memberDetails, xmlDocumentContext );

		JoinColumnProcessing.applyMapKeyJoinColumns(
				jaxbPluralAttribute.getMapKeyJoinColumns(),
				memberDetails,
				xmlDocumentContext
		);

		ForeignKeyProcessing.applyForeignKey( jaxbPluralAttribute.getMapKeyForeignKey(), memberDetails, xmlDocumentContext );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// filters and custom sql

		jaxbPluralAttribute.getFilters().forEach( (jaxbFilter) -> {
			XmlAnnotationHelper.applyFilter( jaxbFilter, memberDetails, xmlDocumentContext );
		} );

		XmlAnnotationHelper.applySqlRestriction( jaxbPluralAttribute.getSqlRestriction(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlInsert(), memberDetails, SQLInsert.class, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlUpdate(), memberDetails, SQLUpdate.class, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlDelete(), memberDetails, SQLDelete.class, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlDeleteAll(), memberDetails, SQLDeleteAll.class, xmlDocumentContext );
	}

	private static void applyOrderColumn(
			JaxbPluralAttribute jaxbPluralAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbOrderColumnImpl jaxbOrderColumn = jaxbPluralAttribute.getOrderColumn();
		if ( jaxbOrderColumn == null ) {
			return;
		}

		final MutableAnnotationUsage<OrderColumn> orderColumnAnn = XmlProcessingHelper.getOrMakeAnnotation(
				OrderColumn.class,
				memberDetails,
				xmlDocumentContext
		);
		final AnnotationDescriptor<OrderColumn> orderColumnDescriptor = xmlDocumentContext.getModelBuildingContext()
				.getAnnotationDescriptorRegistry()
				.getDescriptor( OrderColumn.class );

		applyOr( jaxbOrderColumn, JaxbOrderColumnImpl::getName, "name", orderColumnAnn, orderColumnDescriptor );
		applyOr( jaxbOrderColumn, JaxbOrderColumnImpl::isNullable, "nullable", orderColumnAnn, orderColumnDescriptor );
		applyOr( jaxbOrderColumn, JaxbOrderColumnImpl::isInsertable, "insertable", orderColumnAnn, orderColumnDescriptor );
		applyOr( jaxbOrderColumn, JaxbOrderColumnImpl::isUpdatable, "updatable", orderColumnAnn, orderColumnDescriptor );
		applyOr( jaxbOrderColumn, JaxbOrderColumnImpl::getColumnDefinition, "columnDefinition", orderColumnAnn, orderColumnDescriptor );
		applyOr( jaxbOrderColumn, JaxbOrderColumnImpl::getOptions, "options", orderColumnAnn, orderColumnDescriptor );
	}
}
