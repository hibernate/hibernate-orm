/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.CompositeTypeRegistration;
import org.hibernate.annotations.CompositeTypeRegistrations;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CompositeTypeRegistrationsAnnotation
		implements CompositeTypeRegistrations, RepeatableContainer<CompositeTypeRegistration> {
	private org.hibernate.annotations.CompositeTypeRegistration[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CompositeTypeRegistrationsAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CompositeTypeRegistrationsAnnotation(
			CompositeTypeRegistrations annotation,
			SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue(
				annotation,
				HibernateAnnotations.COMPOSITE_TYPE_REGISTRATIONS,
				"value",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CompositeTypeRegistrationsAnnotation(
			AnnotationInstance annotation,
			SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue(
				annotation,
				HibernateAnnotations.COMPOSITE_TYPE_REGISTRATIONS,
				"value",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CompositeTypeRegistrations.class;
	}

	@Override
	public org.hibernate.annotations.CompositeTypeRegistration[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.CompositeTypeRegistration[] value) {
		this.value = value;
	}


}
