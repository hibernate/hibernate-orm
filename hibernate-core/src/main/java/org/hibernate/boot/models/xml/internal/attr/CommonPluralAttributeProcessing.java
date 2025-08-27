/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.annotations.FetchMode;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOrderColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralFetchModeImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.BatchSizeAnnotation;
import org.hibernate.boot.models.annotations.internal.FetchAnnotation;
import org.hibernate.boot.models.annotations.internal.ListIndexBaseAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyClassJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyEnumeratedJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyTemporalJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderByJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OrderColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.SortComparatorAnnotation;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableMemberDetails;

/**
 * @author Marco Belladelli
 */
public class CommonPluralAttributeProcessing {
	public static void applyPluralAttributeStructure(
			JaxbPluralAttribute jaxbPluralAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final ModelsContext buildingContext = xmlDocumentContext.getModelBuildingContext();
		final ClassDetailsRegistry classDetailsRegistry = buildingContext.getClassDetailsRegistry();

		if ( jaxbPluralAttribute.getFetchMode() != null ) {
			final FetchAnnotation fetchAnn = (FetchAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.FETCH,
					buildingContext
			);
			fetchAnn.value( interpretFetchMode( jaxbPluralAttribute.getFetchMode() ) );
		}

		if ( jaxbPluralAttribute.getClassification() != null ) {
			if ( jaxbPluralAttribute.getClassification() == LimitedCollectionClassification.BAG ) {
				memberDetails.applyAnnotationUsage( HibernateAnnotations.BAG, buildingContext );
			}
			else {
				XmlAnnotationHelper.applyCollectionClassification(
						jaxbPluralAttribute.getClassification(),
						memberDetails,
						xmlDocumentContext
				);
			}
		}

		if ( jaxbPluralAttribute.getBatchSize() != null ) {
			final BatchSizeAnnotation batchSizeAnnotation = (BatchSizeAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.BATCH_SIZE,
					buildingContext
			);
			batchSizeAnnotation.size( jaxbPluralAttribute.getBatchSize() );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// collection-structure

		XmlAnnotationHelper.applyCollectionUserType( jaxbPluralAttribute.getCollectionType(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyCollectionId( jaxbPluralAttribute.getCollectionId(), memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbPluralAttribute.getOrderBy() ) ) {
			final OrderByJpaAnnotation orderByAnn = (OrderByJpaAnnotation) memberDetails.applyAnnotationUsage(
					JpaAnnotations.ORDER_BY,
					buildingContext
			);
			orderByAnn.value( jaxbPluralAttribute.getOrderBy() );
		}

		applyOrderColumn( jaxbPluralAttribute, memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbPluralAttribute.getSort() ) ) {
			final SortComparatorAnnotation sortAnn = (SortComparatorAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.SORT_COMPARATOR,
					buildingContext
			);
			final ClassDetails comparatorClassDetails = classDetailsRegistry.resolveClassDetails( jaxbPluralAttribute.getSort() );
			sortAnn.value( comparatorClassDetails.toJavaClass() );
		}

		if ( jaxbPluralAttribute.getSortNatural() != null ) {
			memberDetails.applyAnnotationUsage( HibernateAnnotations.SORT_NATURAL, buildingContext );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// map-key

		if ( jaxbPluralAttribute.getMapKey() != null ) {
			final MapKeyJpaAnnotation mapKeyAnn = (MapKeyJpaAnnotation) memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAP_KEY,
					buildingContext
			);
			if ( jaxbPluralAttribute.getMapKey() != null && StringHelper.isNotEmpty( jaxbPluralAttribute.getMapKey().getName() ) ) {
				mapKeyAnn.name( jaxbPluralAttribute.getMapKey().getName() );
			}
		}

		if ( jaxbPluralAttribute.getMapKeyClass() != null ) {
			final String className = xmlDocumentContext.resolveClassName( jaxbPluralAttribute.getMapKeyClass().getClazz() );
			final ClassDetails mapKeyClass = classDetailsRegistry.resolveClassDetails( className );
			final MapKeyClassJpaAnnotation mapKeyClassAnn = (MapKeyClassJpaAnnotation) memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAP_KEY_CLASS,
					buildingContext
			);
			mapKeyClassAnn.value( mapKeyClass.toJavaClass() );
		}

		if ( jaxbPluralAttribute.getMapKeyTemporal() != null ) {
			final MapKeyTemporalJpaAnnotation mapKeyTemporalAnn = (MapKeyTemporalJpaAnnotation) memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAP_KEY_TEMPORAL,
					buildingContext
			);
			mapKeyTemporalAnn.value( jaxbPluralAttribute.getMapKeyTemporal() );
		}

		if ( jaxbPluralAttribute.getMapKeyEnumerated() != null ) {
			final MapKeyEnumeratedJpaAnnotation mapKeyEnumeratedAnn = (MapKeyEnumeratedJpaAnnotation) memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAP_KEY_ENUMERATED,
					buildingContext
			);
			mapKeyEnumeratedAnn.value( jaxbPluralAttribute.getMapKeyEnumerated() );
		}

		XmlAnnotationHelper.applyConverts(
				jaxbPluralAttribute.getMapKeyConverts(),
				"key",
				memberDetails,
				xmlDocumentContext
		);

		if ( jaxbPluralAttribute.getMapKeyColumn() != null ) {
			final MapKeyColumnJpaAnnotation columnAnn = (MapKeyColumnJpaAnnotation) memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAP_KEY_COLUMN,
					xmlDocumentContext.getModelBuildingContext()
			);
			columnAnn.apply( jaxbPluralAttribute.getMapKeyColumn(), xmlDocumentContext );
		}

		if ( jaxbPluralAttribute.getMapKeyType() != null ) {
			XmlAnnotationHelper.applyMapKeyUserType( jaxbPluralAttribute.getMapKeyType(), memberDetails, xmlDocumentContext );
		}

		JoinColumnProcessing.applyMapKeyJoinColumns(
				jaxbPluralAttribute.getMapKeyJoinColumns(),
				memberDetails,
				xmlDocumentContext
		);

		ForeignKeyProcessing.applyForeignKey( jaxbPluralAttribute.getMapKeyForeignKey(), memberDetails, xmlDocumentContext );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// filters and custom sql

		XmlAnnotationHelper.applyFilters( jaxbPluralAttribute.getFilters(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applySqlRestriction( jaxbPluralAttribute.getSqlRestriction(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql(
				jaxbPluralAttribute.getSqlInsert(),
				memberDetails,
				HibernateAnnotations.SQL_INSERT,
				xmlDocumentContext
		);
		XmlAnnotationHelper.applyCustomSql(
				jaxbPluralAttribute.getSqlUpdate(),
				memberDetails,
				HibernateAnnotations.SQL_UPDATE,
				xmlDocumentContext
		);
		XmlAnnotationHelper.applyCustomSql(
				jaxbPluralAttribute.getSqlDelete(),
				memberDetails,
				HibernateAnnotations.SQL_DELETE,
				xmlDocumentContext
		);
		XmlAnnotationHelper.applyCustomSql(
				jaxbPluralAttribute.getSqlDeleteAll(),
				memberDetails,
				HibernateAnnotations.SQL_DELETE_ALL,
				xmlDocumentContext
		);
	}

	private static FetchMode interpretFetchMode(JaxbPluralFetchModeImpl fetchMode) {
		return switch ( fetchMode ) {
			case JOIN -> FetchMode.JOIN;
			case SELECT, SUBSELECT -> FetchMode.SELECT;
		};
	}

	private static void applyOrderColumn(
			JaxbPluralAttribute jaxbPluralAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbOrderColumnImpl jaxbOrderColumn = jaxbPluralAttribute.getOrderColumn();
		final Integer listIndexBase = jaxbPluralAttribute.getListIndexBase();
		if ( jaxbOrderColumn != null
				|| listIndexBase != null
				|| jaxbPluralAttribute.getClassification() == LimitedCollectionClassification.LIST ) {
			// apply @OrderColumn in any of these cases
			final OrderColumnJpaAnnotation orderColumnAnn = (OrderColumnJpaAnnotation) memberDetails.applyAnnotationUsage(
					JpaAnnotations.ORDER_COLUMN,
					xmlDocumentContext.getModelBuildingContext()
			);

			if ( jaxbOrderColumn != null ) {
				// apply any explicit config
				orderColumnAnn.apply( jaxbOrderColumn, xmlDocumentContext );
			}
		}

		if ( listIndexBase != null ) {
			final ListIndexBaseAnnotation annUsage = (ListIndexBaseAnnotation) memberDetails.applyAnnotationUsage(
					HibernateAnnotations.LIST_INDEX_BASE,
					xmlDocumentContext.getModelBuildingContext()
			);
			annUsage.value( listIndexBase );
		}
	}
}
