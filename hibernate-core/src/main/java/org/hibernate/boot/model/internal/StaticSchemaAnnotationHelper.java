/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;

import org.hibernate.annotations.schema.StaticColumn;
import org.hibernate.annotations.schema.StaticJoinColumn;
import org.hibernate.annotations.schema.StaticTable;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Adapts generated static schema annotations to the equivalent JPA annotations.
 */
final class StaticSchemaAnnotationHelper {

	private StaticSchemaAnnotationHelper() {
	}

	static Table getTableAnnotation(ClassDetails annotatedClass, MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		final var jpaTable = annotatedClass.getAnnotationUsage( Table.class, modelsContext );
		if ( jpaTable != null ) {
			return jpaTable;
		}

		final var staticTable = locateMetaAnnotation( annotatedClass, StaticTable.class, modelsContext );
		if ( staticTable == null ) {
			return null;
		}

		final TableJpaAnnotation table = new TableJpaAnnotation( modelsContext );
		table.name( staticTable.name() );
		return table;
	}

	static boolean hasTableAnnotation(ClassDetails annotatedClass, MetadataBuildingContext context) {
		return getTableAnnotation( annotatedClass, context ) != null;
	}

	static Column getColumnAnnotation(MemberDetails memberDetails, MetadataBuildingContext context) {
		return getColumnAnnotation( (AnnotationTarget) memberDetails, context );
	}

	static Column getColumnAnnotation(AnnotationTarget annotationTarget, MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		final var jpaColumn = annotationTarget.getAnnotationUsage( Column.class, modelsContext );
		if ( jpaColumn != null ) {
			return jpaColumn;
		}

		final var staticColumn = locateMetaAnnotation( annotationTarget, StaticColumn.class, modelsContext );
		if ( staticColumn == null ) {
			return null;
		}

		final var column = new ColumnJpaAnnotation( modelsContext );
		column.name( staticColumn.name() );
		column.nullable( staticColumn.nullable() );
		column.length( staticColumn.length() );
		column.precision( staticColumn.precision() );
		column.scale( staticColumn.scale() );
		return column;
	}

	static boolean hasColumnAnnotation(MemberDetails memberDetails, MetadataBuildingContext context) {
		return getColumnAnnotation( memberDetails, context ) != null;
	}

	static JoinColumn[] getJoinColumnAnnotations(MemberDetails memberDetails, MetadataBuildingContext context) {
		final var modelsContext = context.getBootstrapContext().getModelsContext();
		final var jpaJoinColumns = memberDetails.getRepeatedAnnotationUsages( JpaAnnotations.JOIN_COLUMN, modelsContext );
		if ( jpaJoinColumns.length > 0 ) {
			return jpaJoinColumns;
		}

		final var staticJoinColumns = getMetaAnnotated( memberDetails, StaticJoinColumn.class, modelsContext );
		if ( staticJoinColumns.isEmpty() ) {
			return null;
		}

		staticJoinColumns.sort( Comparator.comparing( StaticJoinColumn::name ) );
		final var joinColumns = new JoinColumn[staticJoinColumns.size()];
		for ( int i = 0; i < staticJoinColumns.size(); i++ ) {
			joinColumns[i] = toJoinColumn( staticJoinColumns.get( i ), modelsContext );
		}
		return joinColumns;
	}

	static boolean hasJoinColumnAnnotation(MemberDetails memberDetails, MetadataBuildingContext context) {
		final var joinColumns = getJoinColumnAnnotations( memberDetails, context );
		return joinColumns != null && joinColumns.length > 0;
	}

	static boolean isJoinColumnNullable(MemberDetails memberDetails, MetadataBuildingContext context) {
		final var joinColumns = getJoinColumnAnnotations( memberDetails, context );
		if ( joinColumns == null || joinColumns.length == 0 ) {
			return true;
		}
		for ( var joinColumn : joinColumns ) {
			if ( joinColumn.nullable() ) {
				return true;
			}
		}
		return false;
	}

	private static JoinColumn toJoinColumn(StaticJoinColumn staticJoinColumn, ModelsContext modelsContext) {
		final var joinColumn = new JoinColumnJpaAnnotation( modelsContext );
		joinColumn.name( staticJoinColumn.name() );
		joinColumn.referencedColumnName( staticJoinColumn.referencedColumnName() );
		joinColumn.nullable( staticJoinColumn.nullable() );
		return joinColumn;
	}

	private static <A extends Annotation> A locateMetaAnnotation(
			AnnotationTarget annotationTarget,
			Class<A> annotationType,
			ModelsContext modelsContext) {
		final A located = annotationTarget.locateAnnotationUsage( annotationType, modelsContext );
		if ( located != null ) {
			return located;
		}

		for ( var metaAnnotated : annotationTarget.getMetaAnnotated( annotationType, modelsContext ) ) {
			final A usage = metaAnnotated.annotationType().getAnnotation( annotationType );
			if ( usage != null ) {
				return usage;
			}
		}
		return null;
	}

	private static <A extends Annotation> ArrayList<A> getMetaAnnotated(
			AnnotationTarget annotationTarget,
			Class<A> annotationType,
			ModelsContext modelsContext) {
		final var result = new ArrayList<A>();
		for ( var metaAnnotated : annotationTarget.getMetaAnnotated( annotationType, modelsContext ) ) {
			final A usage = metaAnnotated.annotationType().getAnnotation( annotationType );
			if ( usage != null ) {
				result.add( usage );
			}
		}
		return result;
	}
}
