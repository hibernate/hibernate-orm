/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.schema.StaticColumn;
import org.hibernate.annotations.schema.StaticTable;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.TableJpaAnnotation;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Column;
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
}
