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
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrimaryKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnJoined;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;

/**
 * XML -> AnnotationUsage support for {@linkplain JaxbColumnJoined}
 *
 * @author Steve Ebersole
 */
public class JoinColumnProcessing {
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

		final MutableAnnotationUsage<PrimaryKeyJoinColumns> columnsUsage = memberDetails.applyAnnotationUsage(
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMNS,
				xmlDocumentContext.getModelBuildingContext()
		);
		final List<MutableAnnotationUsage<PrimaryKeyJoinColumn>> columnUsages = CollectionHelper.arrayList( jaxbJoinColumns.size() );
		columnsUsage.setAttributeValue( "value", columnUsages );

		jaxbJoinColumns.forEach( (jaxbJoinColumn) -> {
			final MutableAnnotationUsage<PrimaryKeyJoinColumn> columnUsage = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage(
					memberDetails,
					xmlDocumentContext.getModelBuildingContext()
			);
			columnUsages.add( columnUsage );

			transferJoinColumn(
					jaxbJoinColumn,
					columnUsage,
					memberDetails,
					xmlDocumentContext
			);
		} );
	}

	private static void transferJoinColumn(
			JaxbColumnJoined jaxbJoinColumn,
			MutableAnnotationUsage<? extends Annotation> joinColumnUsage,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		ColumnProcessing.applyColumnDetails( jaxbJoinColumn, memberDetails, joinColumnUsage, xmlDocumentContext );
		XmlAnnotationHelper.applyOptionalAttribute(
				joinColumnUsage,
				"referencedColumnName",
				jaxbJoinColumn.getReferencedColumnName()
		);

		final JaxbForeignKeyImpl jaxbForeignKey = jaxbJoinColumn.getForeignKey();
		if ( jaxbForeignKey != null ) {
			joinColumnUsage.setAttributeValue(
					"foreignKey",
					ForeignKeyProcessing.createNestedForeignKeyAnnotation( jaxbForeignKey, memberDetails, xmlDocumentContext )
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
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return Collections.emptyList();
		}
		final List<AnnotationUsage<JoinColumn>> joinColumns = new ArrayList<>( jaxbJoinColumns.size() );
		jaxbJoinColumns.forEach( jaxbJoinColumn -> {
			joinColumns.add( createJoinColumnAnnotation( jaxbJoinColumn, JoinColumn.class, xmlDocumentContext ) );
		} );
		return joinColumns;
	}

	public static <A extends Annotation> MutableAnnotationUsage<A> createJoinColumnAnnotation(
			JaxbColumnJoined jaxbJoinColumn,
			MutableMemberDetails memberDetails,
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<A> joinColumnAnn = XmlProcessingHelper.getOrMakeNamedAnnotation(
				annotationType,
				jaxbJoinColumn.getName(),
				memberDetails,
				xmlDocumentContext
		);
		transferJoinColumn( jaxbJoinColumn, joinColumnAnn, memberDetails, xmlDocumentContext );
		return joinColumnAnn;
	}

	public static <A extends Annotation> MutableAnnotationUsage<A> createJoinColumnAnnotation(
			JaxbColumnJoined jaxbJoinColumn,
			Class<A> annotationType,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<A> joinColumnAnn = XmlProcessingHelper.getOrMakeAnnotation( annotationType, xmlDocumentContext );
		transferJoinColumn( jaxbJoinColumn, joinColumnAnn, null, xmlDocumentContext );
		return joinColumnAnn;
	}

	public static List<AnnotationUsage<JoinColumn>> createJoinColumns(
			List<JaxbJoinColumnImpl> jaxbJoinColumns,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return Collections.emptyList();
		}
		final List<AnnotationUsage<JoinColumn>> joinColumns = new ArrayList<>( jaxbJoinColumns.size() );
		jaxbJoinColumns.forEach( jaxbJoinColumn -> {
			final MutableAnnotationUsage<JoinColumn> joinColumnUsage = JpaAnnotations.JOIN_COLUMN.createUsage(
					memberDetails,
					xmlDocumentContext.getModelBuildingContext()
			);
			transferJoinColumn(
					jaxbJoinColumn,
					joinColumnUsage,
					memberDetails,
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

		if ( jaxbJoinColumns.size() == 1 ) {
			XmlAnnotationHelper.applyJoinColumn( jaxbJoinColumns.get( 0 ), memberDetails, xmlDocumentContext );
		}
		else {
			final MutableAnnotationUsage<JoinColumns> columnsAnn = XmlProcessingHelper.makeAnnotation(
					JoinColumns.class,
					memberDetails,
					xmlDocumentContext
			);
			columnsAnn.setAttributeValue( "value", createJoinColumns( jaxbJoinColumns, memberDetails, xmlDocumentContext ) );
		}
	}
}
