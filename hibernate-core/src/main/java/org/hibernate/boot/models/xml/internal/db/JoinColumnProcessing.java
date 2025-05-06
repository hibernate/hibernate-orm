/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal.db;

import java.io.Serializable;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrimaryKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbColumnJoined;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnOrFormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsOrFormulasAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinFormulaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.MapKeyJoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnsJpaAnnotation;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyJoinColumn;
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

	public static void applyJoinColumns(
			List<JaxbJoinColumnImpl> jaxbJoinColumns,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return;
		}

		final JoinColumnsJpaAnnotation columnsAnn = (JoinColumnsJpaAnnotation) memberDetails.replaceAnnotationUsage(
				JpaAnnotations.JOIN_COLUMN,
				JpaAnnotations.JOIN_COLUMNS,
				xmlDocumentContext.getModelBuildingContext()
		);
		columnsAnn.value( transformJoinColumnList(
				jaxbJoinColumns,
				xmlDocumentContext
		) );
	}

	private static final JoinColumn[] NO_JOIN_COLUMNS = new JoinColumn[0];

	public static JoinColumn[] transformJoinColumnList(
			List<JaxbJoinColumnImpl> jaxbJoinColumns,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return NO_JOIN_COLUMNS;
		}

		final JoinColumn[] joinColumns = new JoinColumn[jaxbJoinColumns.size()];
		for ( int i = 0; i < jaxbJoinColumns.size(); i++ ) {
			final JoinColumnJpaAnnotation joinColumn = JpaAnnotations.JOIN_COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );
			joinColumns[i] = joinColumn;
			joinColumn.apply( jaxbJoinColumns.get( i ), xmlDocumentContext );
		}
		return joinColumns;
	}

	public static void applyJoinColumnsOrFormulas(
			List<Serializable> jaxbJoinColumnsOrFormulas,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( !CollectionHelper.isEmpty( jaxbJoinColumnsOrFormulas ) ) {
			memberDetails.removeAnnotationUsage( JpaAnnotations.JOIN_COLUMN );
			memberDetails.removeAnnotationUsage( JpaAnnotations.JOIN_COLUMNS );
			final JoinColumnsOrFormulasAnnotation joinColumnsOrFormulasUsage = (JoinColumnsOrFormulasAnnotation) memberDetails.replaceAnnotationUsage(
					HibernateAnnotations.JOIN_COLUMN_OR_FORMULA,
					HibernateAnnotations.JOIN_COLUMNS_OR_FORMULAS,
					xmlDocumentContext.getModelBuildingContext()
			);

			final JoinColumnOrFormula[] joinColumnOrFormulaList = new JoinColumnOrFormula[jaxbJoinColumnsOrFormulas.size()];
			joinColumnsOrFormulasUsage.value( joinColumnOrFormulaList );

			for ( int i = 0; i < jaxbJoinColumnsOrFormulas.size(); i++ ) {
				final JoinColumnOrFormulaAnnotation joinColumnOrFormulaUsage = HibernateAnnotations.JOIN_COLUMN_OR_FORMULA.createUsage(
						xmlDocumentContext.getModelBuildingContext() );
				joinColumnOrFormulaList[i] = joinColumnOrFormulaUsage;

				final Serializable jaxbJoinColumnOrFormula = jaxbJoinColumnsOrFormulas.get( i );
				if ( jaxbJoinColumnOrFormula instanceof JaxbJoinColumnImpl jaxbJoinColumn ) {
					final JoinColumnJpaAnnotation joinColumnUsage = JpaAnnotations.JOIN_COLUMN.createUsage(
							xmlDocumentContext.getModelBuildingContext() );
					joinColumnOrFormulaUsage.column( joinColumnUsage );
					joinColumnUsage.apply( jaxbJoinColumn, xmlDocumentContext );
				}
				else if ( jaxbJoinColumnOrFormula instanceof String jaxbJoinFormula ) {
					final JoinFormulaAnnotation joinFormulaUsage = HibernateAnnotations.JOIN_FORMULA.createUsage(
							xmlDocumentContext.getModelBuildingContext() );
					joinColumnOrFormulaUsage.formula( joinFormulaUsage );
					joinFormulaUsage.value( jaxbJoinFormula );
				}
				else {
					throw new MappingException( "Unexpected join-column-or-formula type : " + jaxbJoinColumnOrFormula );
				}
			}
		}
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

		final PrimaryKeyJoinColumnsJpaAnnotation columnsUsage = (PrimaryKeyJoinColumnsJpaAnnotation) memberDetails.replaceAnnotationUsage(
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN,
				JpaAnnotations.PRIMARY_KEY_JOIN_COLUMNS,
				xmlDocumentContext.getModelBuildingContext()
		);

		columnsUsage.value( transformPrimaryKeyJoinColumns(
				jaxbJoinColumns,
				xmlDocumentContext
		) );
	}

	private static final PrimaryKeyJoinColumn[] NO_PRIMARY_KEY_JOIN_COLUMNS = new PrimaryKeyJoinColumn[0];
	public static PrimaryKeyJoinColumn[] transformPrimaryKeyJoinColumns(
			List<JaxbPrimaryKeyJoinColumnImpl> jaxbJoinColumns,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return NO_PRIMARY_KEY_JOIN_COLUMNS;
		}

		final PrimaryKeyJoinColumn[] result = new PrimaryKeyJoinColumn[jaxbJoinColumns.size()];
		for ( int i = 0; i < jaxbJoinColumns.size(); i++ ) {
			final PrimaryKeyJoinColumnJpaAnnotation joinColumn = JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );
			result[i] = joinColumn;

			joinColumn.apply( jaxbJoinColumns.get(i), xmlDocumentContext );
		}
		return result;
	}

	public static void applyMapKeyJoinColumns(
			List<JaxbMapKeyJoinColumnImpl> jaxbJoinColumns,
			MutableMemberDetails memberDetails,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return;
		}

		final MapKeyJoinColumnsJpaAnnotation joinColumnsUsage = (MapKeyJoinColumnsJpaAnnotation) memberDetails.replaceAnnotationUsage(
				JpaAnnotations.MAP_KEY_JOIN_COLUMN,
				JpaAnnotations.MAP_KEY_JOIN_COLUMNS,
				xmlDocumentContext.getModelBuildingContext()
		);
		joinColumnsUsage.value( transformMapKeyJoinColumns( jaxbJoinColumns, xmlDocumentContext ) );
	}

	private static final MapKeyJoinColumn[] NO_KEY_JOIN_COLUMNS = new MapKeyJoinColumn[0];
	private static MapKeyJoinColumn[] transformMapKeyJoinColumns(
			List<JaxbMapKeyJoinColumnImpl> jaxbJoinColumns,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbJoinColumns ) ) {
			return NO_KEY_JOIN_COLUMNS;
		}

		final MapKeyJoinColumn[] joinColumns = new MapKeyJoinColumn[jaxbJoinColumns.size()];
		for ( int i = 0; i < jaxbJoinColumns.size(); i++ ) {
			final MapKeyJoinColumnJpaAnnotation annotation = JpaAnnotations.MAP_KEY_JOIN_COLUMN.createUsage( xmlDocumentContext.getModelBuildingContext() );
			joinColumns[i] = annotation;
			annotation.apply( jaxbJoinColumns.get(i), xmlDocumentContext );
		}
		return joinColumns;
	}
}
