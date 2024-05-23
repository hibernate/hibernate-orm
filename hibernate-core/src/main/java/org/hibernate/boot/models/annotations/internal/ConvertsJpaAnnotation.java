/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import java.lang.annotation.Annotation;

import jakarta.persistence.Converts;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConvertsJpaAnnotation implements Converts, RepeatableContainer<jakarta.persistence.Convert> {
	private jakarta.persistence.Convert[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConvertsJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConvertsJpaAnnotation(Converts annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				org.hibernate.boot.models.JpaAnnotations.CONVERTS,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConvertsJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue(
				annotation,
				org.hibernate.boot.models.JpaAnnotations.CONVERTS,
				"value",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Converts.class;
	}

	@Override
	public jakarta.persistence.Convert[] value() {
		return value;
	}

	public void value(jakarta.persistence.Convert[] value) {
		this.value = value;
	}


}
