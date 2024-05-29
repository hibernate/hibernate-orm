/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class DiscriminatorFormulaAnnotation implements DiscriminatorFormula {
	private String value;
	private jakarta.persistence.DiscriminatorType discriminatorType;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public DiscriminatorFormulaAnnotation(SourceModelBuildingContext modelContext) {
		this.discriminatorType = jakarta.persistence.DiscriminatorType.STRING;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public DiscriminatorFormulaAnnotation(DiscriminatorFormula annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
		this.discriminatorType = annotation.discriminatorType();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public DiscriminatorFormulaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue(
				annotation,
				org.hibernate.boot.models.HibernateAnnotations.DISCRIMINATOR_FORMULA,
				"value",
				modelContext
		);
		this.discriminatorType = extractJandexValue(
				annotation,
				org.hibernate.boot.models.HibernateAnnotations.DISCRIMINATOR_FORMULA,
				"discriminatorType",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DiscriminatorFormula.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}


	@Override
	public jakarta.persistence.DiscriminatorType discriminatorType() {
		return discriminatorType;
	}

	public void discriminatorType(jakarta.persistence.DiscriminatorType value) {
		this.discriminatorType = value;
	}


}
