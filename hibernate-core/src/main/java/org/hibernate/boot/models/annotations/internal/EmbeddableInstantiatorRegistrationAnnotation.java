/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.EmbeddableInstantiatorRegistration;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class EmbeddableInstantiatorRegistrationAnnotation implements EmbeddableInstantiatorRegistration {
	private java.lang.Class<?> embeddableClass;
	private java.lang.Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> instantiator;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public EmbeddableInstantiatorRegistrationAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public EmbeddableInstantiatorRegistrationAnnotation(
			EmbeddableInstantiatorRegistration annotation,
			SourceModelBuildingContext modelContext) {
		this.embeddableClass = annotation.embeddableClass();
		this.instantiator = annotation.instantiator();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public EmbeddableInstantiatorRegistrationAnnotation(
			AnnotationInstance annotation,
			SourceModelBuildingContext modelContext) {
		this.embeddableClass = extractJandexValue(
				annotation,
				HibernateAnnotations.EMBEDDABLE_INSTANTIATOR_REGISTRATION,
				"embeddableClass",
				modelContext
		);
		this.instantiator = extractJandexValue(
				annotation,
				HibernateAnnotations.EMBEDDABLE_INSTANTIATOR_REGISTRATION,
				"instantiator",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return EmbeddableInstantiatorRegistration.class;
	}

	@Override
	public java.lang.Class<?> embeddableClass() {
		return embeddableClass;
	}

	public void embeddableClass(java.lang.Class<?> value) {
		this.embeddableClass = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> instantiator() {
		return instantiator;
	}

	public void instantiator(java.lang.Class<? extends org.hibernate.metamodel.spi.EmbeddableInstantiator> value) {
		this.instantiator = value;
	}


}
