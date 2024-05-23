/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.ColumnTransformers;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ColumnTransformersAnnotation implements ColumnTransformers, RepeatableContainer<ColumnTransformer> {
	private org.hibernate.annotations.ColumnTransformer[] value;

	public ColumnTransformersAnnotation(SourceModelBuildingContext modelContext) {
	}

	public ColumnTransformersAnnotation(ColumnTransformers annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.COLUMN_TRANSFORMERS, "value", modelContext );
	}

	public ColumnTransformersAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, HibernateAnnotations.COLUMN_TRANSFORMERS, "value", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ColumnTransformers.class;
	}

	@Override
	public org.hibernate.annotations.ColumnTransformer[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.ColumnTransformer[] value) {
		this.value = value;
	}


}
