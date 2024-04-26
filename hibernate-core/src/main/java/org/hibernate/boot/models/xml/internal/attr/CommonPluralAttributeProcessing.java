/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.attr;

import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.internal.CollectionClassification;
import org.hibernate.boot.internal.LimitedCollectionClassification;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOrderColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
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

import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
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
			final MutableAnnotationUsage<Fetch> fetchAnn = memberDetails.applyAnnotationUsage(
					HibernateAnnotations.FETCH,
					buildingContext
			);
			fetchAnn.setAttributeValue( "value", jaxbPluralAttribute.getFetchMode() );
		}

		if ( jaxbPluralAttribute.getClassification() != null ) {
			if ( jaxbPluralAttribute.getClassification() == LimitedCollectionClassification.BAG ) {
				memberDetails.applyAnnotationUsage( HibernateAnnotations.BAG, buildingContext );
			}
			else {
				final MutableAnnotationUsage<CollectionClassification> collectionClassificationAnn = memberDetails.applyAnnotationUsage(
						HibernateAnnotations.COLLECTION_CLASSIFICATION,
						buildingContext
				);
				collectionClassificationAnn.setAttributeValue( "value", jaxbPluralAttribute.getClassification() );
			}
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// collection-structure

		XmlAnnotationHelper.applyCollectionUserType( jaxbPluralAttribute.getCollectionType(), memberDetails, xmlDocumentContext );

		XmlAnnotationHelper.applyCollectionId( jaxbPluralAttribute.getCollectionId(), memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbPluralAttribute.getOrderBy() ) ) {
			final MutableAnnotationUsage<OrderBy> orderByAnn = memberDetails.applyAnnotationUsage(
					JpaAnnotations.ORDER_BY,
					buildingContext
			);
			orderByAnn.setAttributeValue( "value", jaxbPluralAttribute.getOrderBy() );
		}

		applyOrderColumn( jaxbPluralAttribute, memberDetails, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbPluralAttribute.getSort() ) ) {
			final MutableAnnotationUsage<SortComparator> sortAnn = memberDetails.applyAnnotationUsage(
					HibernateAnnotations.SORT_COMPARATOR,
					buildingContext
			);
			final ClassDetails comparatorClassDetails = classDetailsRegistry.resolveClassDetails( jaxbPluralAttribute.getSort() );
			sortAnn.setAttributeValue( "value", comparatorClassDetails );
		}

		if ( jaxbPluralAttribute.getSortNatural() != null ) {
			memberDetails.applyAnnotationUsage( HibernateAnnotations.SORT_NATURAL, buildingContext );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// map-key

		if ( jaxbPluralAttribute.getMapKey() != null ) {
			final MutableAnnotationUsage<MapKey> mapKeyAnn = memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAP_KEY,
					buildingContext
			);
			if ( jaxbPluralAttribute.getMapKey() != null ) {
				XmlAnnotationHelper.applyOptionalAttribute( mapKeyAnn, "name", jaxbPluralAttribute.getMapKey().getName() );
			}
		}

		if ( jaxbPluralAttribute.getMapKeyClass() != null ) {
			final String className = xmlDocumentContext.resolveClassName( jaxbPluralAttribute.getMapKeyClass().getClazz() );
			final ClassDetails mapKeyClass = classDetailsRegistry.resolveClassDetails( className );
			final MutableAnnotationUsage<MapKeyClass> mapKeyClassAnn = memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAP_KEY_CLASS,
					buildingContext
			);
			mapKeyClassAnn.setAttributeValue( "value", mapKeyClass );
		}

		if ( jaxbPluralAttribute.getMapKeyTemporal() != null ) {
			final MutableAnnotationUsage<MapKeyTemporal> mapKeyTemporalAnn = memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAP_KEY_TEMPORAL,
					buildingContext
			);
			mapKeyTemporalAnn.setAttributeValue( "value", jaxbPluralAttribute.getMapKeyTemporal() );
		}

		if ( jaxbPluralAttribute.getMapKeyEnumerated() != null ) {
			final MutableAnnotationUsage<MapKeyEnumerated> mapKeyEnumeratedAnn = memberDetails.applyAnnotationUsage(
					JpaAnnotations.MAP_KEY_ENUMERATED,
					buildingContext
			);
			mapKeyEnumeratedAnn.setAttributeValue( "value", jaxbPluralAttribute.getMapKeyEnumerated() );
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

		XmlAnnotationHelper.applyFilters( jaxbPluralAttribute.getFilters(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applySqlRestriction( jaxbPluralAttribute.getSqlRestriction(), memberDetails, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlInsert(), memberDetails, HibernateAnnotations.SQL_INSERT, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlUpdate(), memberDetails, HibernateAnnotations.SQL_UPDATE, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlDelete(), memberDetails, HibernateAnnotations.SQL_DELETE, xmlDocumentContext );
		XmlAnnotationHelper.applyCustomSql( jaxbPluralAttribute.getSqlDeleteAll(), memberDetails, HibernateAnnotations.SQL_DELETE_ALL, xmlDocumentContext );
	}

	private static void applyOrderColumn(
			JaxbPluralAttribute jaxbPluralAttribute,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		final JaxbOrderColumnImpl jaxbOrderColumn = jaxbPluralAttribute.getOrderColumn();
		if ( jaxbOrderColumn == null ) {
			return;
		}

		final MutableAnnotationUsage<OrderColumn> orderColumnAnn = memberDetails.applyAnnotationUsage(
				JpaAnnotations.ORDER_COLUMN,
				xmlDocumentContext.getModelBuildingContext()
		);

		XmlAnnotationHelper.applyOptionalAttribute( orderColumnAnn, "name", jaxbOrderColumn.getName() );
		XmlAnnotationHelper.applyOptionalAttribute( orderColumnAnn, "nullable", jaxbOrderColumn.isNullable() );
		XmlAnnotationHelper.applyOptionalAttribute( orderColumnAnn, "insertable", jaxbOrderColumn.isInsertable() );
		XmlAnnotationHelper.applyOptionalAttribute( orderColumnAnn, "updatable", jaxbOrderColumn.isUpdatable() );
		XmlAnnotationHelper.applyOptionalAttribute( orderColumnAnn, "columnDefinition", jaxbOrderColumn.getColumnDefinition() );
		XmlAnnotationHelper.applyOptionalAttribute( orderColumnAnn, "options", jaxbOrderColumn.getOptions() );
	}
}
