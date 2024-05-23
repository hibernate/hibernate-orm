/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.Convert;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ConvertJpaAnnotation implements Convert {

	private java.lang.Class<? extends jakarta.persistence.AttributeConverter> converter;
	private String attributeName;
	private boolean disableConversion;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ConvertJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.converter = jakarta.persistence.AttributeConverter.class;
		this.attributeName = "";
		this.disableConversion = false;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ConvertJpaAnnotation(Convert annotation, SourceModelBuildingContext modelContext) {
		this.converter = extractJdkValue( annotation, JpaAnnotations.CONVERT, "converter", modelContext );
		this.attributeName = extractJdkValue( annotation, JpaAnnotations.CONVERT, "attributeName", modelContext );
		this.disableConversion = extractJdkValue(
				annotation,
				JpaAnnotations.CONVERT,
				"disableConversion",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ConvertJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.converter = extractJandexValue( annotation, JpaAnnotations.CONVERT, "converter", modelContext );
		this.attributeName = extractJandexValue( annotation, JpaAnnotations.CONVERT, "attributeName", modelContext );
		this.disableConversion = extractJandexValue(
				annotation,
				JpaAnnotations.CONVERT,
				"disableConversion",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Convert.class;
	}

	@Override
	public java.lang.Class<? extends jakarta.persistence.AttributeConverter> converter() {
		return converter;
	}

	public void converter(java.lang.Class<? extends jakarta.persistence.AttributeConverter> value) {
		this.converter = value;
	}


	@Override
	public String attributeName() {
		return attributeName;
	}

	public void attributeName(String value) {
		this.attributeName = value;
	}


	@Override
	public boolean disableConversion() {
		return disableConversion;
	}

	public void disableConversion(boolean value) {
		this.disableConversion = value;
	}


}
