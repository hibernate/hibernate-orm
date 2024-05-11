/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal.db;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbForeignKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrimaryKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnJoined;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;

/**
 * XML -> AnnotationUsage support for {@linkplain JaxbColumnJoined}: <ul>
 *     <li>{@code <join-column/>}</li>
 *     <li>{@code <primary-key-join-column/>}</li>
 *     <li>{@code <map-key-join-column/>}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class JoinColumnProcessing {

	public static void applyMapKeyJoinColumns(
			List<JaxbMapKeyJoinColumnImpl> jaxbJoinColumns,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return;
		}

		final MutableAnnotationUsage<MapKeyJoinColumns> joinColumnsUsage = memberDetails.replaceAnnotationUsage(
				JpaAnnotations.MAP_KEY_JOIN_COLUMN,
				JpaAnnotations.MAP_KEY_JOIN_COLUMNS,
				xmlDocumentContext.getModelBuildingContext()
		);
		final ArrayList<MutableAnnotationUsage<MapKeyJoinColumn>> joinColumnUsages = CollectionHelper.arrayList( jaxbJoinColumns.size() );
		joinColumnsUsage.setAttributeValue( "value", joinColumnUsages );
		jaxbJoinColumns.forEach( (jaxbJoinColumn) -> {
			final MutableAnnotationUsage<MapKeyJoinColumn> joinColumnUsage =
					JpaAnnotations.MAP_KEY_JOIN_COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );
			joinColumnUsages.add( joinColumnUsage );
			transferJoinColumn( jaxbJoinColumn, joinColumnUsage, memberDetails, xmlDocumentContext );
		} );
	}

	/**
	 * Support for {@linkplain JaxbPrimaryKeyJoinColumnImpl} to {@linkplain PrimaryKeyJoinColumns} transformation
	 *
	 * @see JaxbPrimaryKeyJoinColumnImpl
	 */
	public static void applyPrimaryKeyJoinColumns(
			List<JaxbPrimaryKeyJoinColumnImpl> jaxbJoinColumns,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return;
		}

		final MutableAnnotationUsage<PrimaryKeyJoinColumns> columnsUsage = memberDetails.replaceAnnotationUsage(
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMNS,
				xmlDocumentContext.getModelBuildingContext()
		);
		final List<MutableAnnotationUsage<PrimaryKeyJoinColumn>> columnUsages = CollectionHelper.arrayList( jaxbJoinColumns.size() );
		columnsUsage.setAttributeValue( "value", columnUsages );

		jaxbJoinColumns.forEach( (jaxbJoinColumn) -> {
			final MutableAnnotationUsage<PrimaryKeyJoinColumn> columnUsage =
					JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );
			columnUsages.add( columnUsage );

			transferJoinColumn(
					jaxbJoinColumn,
					columnUsage,
					memberDetails,
					xmlDocumentContext
			);
		} );
	}

	public static void transferJoinColumn(
			JaxbColumnJoined jaxbJoinColumn,
			MutableAnnotationUsage<? extends Annotation> joinColumnUsage,
			AnnotationTarget annotationTarget,
			XmlDocumentContext xmlDocumentContext) {
		ColumnProcessing.applyColumnDetails( jaxbJoinColumn, annotationTarget, joinColumnUsage, xmlDocumentContext );
		XmlAnnotationHelper.applyOptionalAttribute(
				joinColumnUsage,
				"referencedColumnName",
				jaxbJoinColumn.getReferencedColumnName()
		);

		final JaxbForeignKeyImpl jaxbForeignKey = jaxbJoinColumn.getForeignKey();
		if ( jaxbForeignKey != null ) {
			joinColumnUsage.setAttributeValue(
					"foreignKey",
					ForeignKeyProcessing.createNestedForeignKeyAnnotation( jaxbForeignKey, annotationTarget, xmlDocumentContext )
			);
		}
	}

	/**
	 * Support for {@linkplain JaxbJoinColumnImpl} to {@linkplain JoinColumn} transformation.
	 * Used when the List is the value of an annotation attribute other than its repetition container.
	 * For example, {@linkplain CollectionTable#joinColumns()}, {@linkplain JoinTable#joinColumns()} and
	 * {@linkplain JoinTable#inverseJoinColumns()}
	 */
	public static List<AnnotationUsage<JoinColumn>> transformJoinColumnList(
			List<JaxbJoinColumnImpl> jaxbJoinColumns,
			AnnotationTarget annotationTarget,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return Collections.emptyList();
		}
		final List<AnnotationUsage<JoinColumn>> joinColumns = new ArrayList<>( jaxbJoinColumns.size() );
		jaxbJoinColumns.forEach( jaxbJoinColumn -> {
			final MutableAnnotationUsage<JoinColumn> joinColumnAnn =
					JpaAnnotations.JOIN_COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );
			transferJoinColumn( jaxbJoinColumn, joinColumnAnn, null, xmlDocumentContext );
			joinColumns.add( joinColumnAnn );
		} );
		return joinColumns;
	}

	public static List<AnnotationUsage<JoinColumn>> createJoinColumns(
			List<JaxbJoinColumnImpl> jaxbJoinColumns,
			AnnotationTarget annotationTarget,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return Collections.emptyList();
		}
		final List<AnnotationUsage<JoinColumn>> joinColumns = new ArrayList<>( jaxbJoinColumns.size() );
		jaxbJoinColumns.forEach( jaxbJoinColumn -> {
			final MutableAnnotationUsage<JoinColumn> joinColumnUsage = JpaAnnotations.JOIN_COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );
			transferJoinColumn(
					jaxbJoinColumn,
					joinColumnUsage,
					annotationTarget,
					xmlDocumentContext
			);
			joinColumns.add( joinColumnUsage );
		} );
		return joinColumns;
	}

	public static void applyJoinColumns(
			List<JaxbJoinColumnImpl> jaxbJoinColumns,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return;
		}

		final MutableAnnotationUsage<JoinColumns> columnsAnn = memberDetails.replaceAnnotationUsage(
				JpaAnnotations.JOIN_COLUMN,
				JpaAnnotations.JOIN_COLUMNS,
				xmlDocumentContext.getModelBuildingContext()
		);
		columnsAnn.setAttributeValue( "value", createJoinColumns( jaxbJoinColumns, memberDetails, xmlDocumentContext ) );
	}

	public static <A extends Annotation> void applyPrimaryKeyJoinColumns(
			List<JaxbPrimaryKeyJoinColumnImpl> jaxbJoinColumns,
			MutableAnnotationUsage<A> tableAnn,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return;
		}

		final List<MutableAnnotationUsage<PrimaryKeyJoinColumn>> columnUsages = CollectionHelper.arrayList( jaxbJoinColumns.size() );

		jaxbJoinColumns.forEach(
				(jaxbJoinColumn) ->
						columnUsages.add( createPrimaryKeyJoinColumnUsage( jaxbJoinColumn, xmlDocumentContext ) )
		);

		tableAnn.setAttributeValue(
				"pkJoinColumns",
				columnUsages
		);
	}

	public static MutableAnnotationUsage<PrimaryKeyJoinColumn> createPrimaryKeyJoinColumnUsage(
			JaxbPrimaryKeyJoinColumnImpl jaxbJoinColumn,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<PrimaryKeyJoinColumn> columnUsage =
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );

		ColumnProcessing.applyColumnDetails(
				jaxbJoinColumn,
				(MutableAnnotationUsage<? extends Annotation>) columnUsage,
				xmlDocumentContext
		);

		XmlAnnotationHelper.applyOptionalAttribute(
				columnUsage,
				"referencedColumnName",
				((JaxbColumnJoined) jaxbJoinColumn ).getReferencedColumnName()
		);

		ForeignKeyProcessing.applyForeignKey(
				( (JaxbColumnJoined) jaxbJoinColumn ).getForeignKey(),
				(MutableAnnotationUsage<? extends Annotation>) columnUsage,
				xmlDocumentContext
		);
		return columnUsage;
	}

}
