/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;

import org.hibernate.annotations.schema.ColumnMapping;
import org.hibernate.annotations.schema.JoinColumnMapping;
import org.hibernate.annotations.schema.TableMapping;
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

		final var tableMapping = locateMetaAnnotation( annotatedClass, TableMapping.class, modelsContext );
		if ( tableMapping == null ) {
			return null;
		}

		return new TableJpaAnnotation( tableMapping.value(), modelsContext );
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

		final var columnMapping = locateMetaAnnotation( annotationTarget, ColumnMapping.class, modelsContext );
		if ( columnMapping == null ) {
			return null;
		}

		return new ColumnJpaAnnotation( columnMapping.value(), modelsContext );
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

		final var joinColumnMappings = getMetaAnnotated( memberDetails, JoinColumnMapping.class, modelsContext );
		if ( joinColumnMappings.isEmpty() ) {
			return null;
		}

		joinColumnMappings.sort( Comparator.comparing( joinColumnMapping -> joinColumnMapping.value().name() ) );
		final var joinColumns = new JoinColumn[joinColumnMappings.size()];
		for ( int i = 0; i < joinColumnMappings.size(); i++ ) {
			joinColumns[i] = new JoinColumnJpaAnnotation( joinColumnMappings.get( i ).value(), modelsContext );
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
