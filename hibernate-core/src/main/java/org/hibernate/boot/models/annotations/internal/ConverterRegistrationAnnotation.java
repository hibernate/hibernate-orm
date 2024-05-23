/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.ConverterRegistration;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConverterRegistrationAnnotation implements ConverterRegistration {
	private java.lang.Class<? extends jakarta.persistence.AttributeConverter<?, ?>> converter;
	private java.lang.Class<?> domainType;
	private boolean autoApply;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConverterRegistrationAnnotation(SourceModelBuildingContext modelContext) {
		this.domainType = void.class;
		this.autoApply = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConverterRegistrationAnnotation(ConverterRegistration annotation, SourceModelBuildingContext modelContext) {
		this.converter = extractJdkValue(
				annotation,
				HibernateAnnotations.CONVERTER_REGISTRATION,
				"converter",
				modelContext
		);
		this.domainType = extractJdkValue(
				annotation,
				HibernateAnnotations.CONVERTER_REGISTRATION,
				"domainType",
				modelContext
		);
		this.autoApply = extractJdkValue(
				annotation,
				HibernateAnnotations.CONVERTER_REGISTRATION,
				"autoApply",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConverterRegistrationAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.converter = extractJandexValue(
				annotation,
				HibernateAnnotations.CONVERTER_REGISTRATION,
				"converter",
				modelContext
		);
		this.domainType = extractJandexValue(
				annotation,
				HibernateAnnotations.CONVERTER_REGISTRATION,
				"domainType",
				modelContext
		);
		this.autoApply = extractJandexValue(
				annotation,
				HibernateAnnotations.CONVERTER_REGISTRATION,
				"autoApply",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ConverterRegistration.class;
	}

	@Override
	public java.lang.Class<? extends jakarta.persistence.AttributeConverter<?, ?>> converter() {
		return converter;
	}

	public void converter(java.lang.Class<? extends jakarta.persistence.AttributeConverter<?, ?>> value) {
		this.converter = value;
	}


	@Override
	public java.lang.Class<?> domainType() {
		return domainType;
	}

	public void domainType(java.lang.Class<?> value) {
		this.domainType = value;
	}


	@Override
	public boolean autoApply() {
		return autoApply;
	}

	public void autoApply(boolean value) {
		this.autoApply = value;
	}


}
