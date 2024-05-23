/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JoinColumnOrFormulaAnnotation implements JoinColumnOrFormula {
	private org.hibernate.annotations.JoinFormula formula;
	private jakarta.persistence.JoinColumn column;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JoinColumnOrFormulaAnnotation(SourceModelBuildingContext modelContext) {
		this.formula = modelContext.getAnnotationDescriptorRegistry()
				.getDescriptor( org.hibernate.annotations.JoinFormula.class )
				.createUsage( modelContext );
		this.column = modelContext.getAnnotationDescriptorRegistry()
				.getDescriptor( jakarta.persistence.JoinColumn.class )
				.createUsage( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JoinColumnOrFormulaAnnotation(JoinColumnOrFormula annotation, SourceModelBuildingContext modelContext) {
		this.formula = extractJdkValue(
				annotation,
				HibernateAnnotations.JOIN_COLUMN_OR_FORMULA,
				"formula",
				modelContext
		);
		this.column = extractJdkValue(
				annotation,
				HibernateAnnotations.JOIN_COLUMN_OR_FORMULA,
				"column",
				modelContext
		);
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JoinColumnOrFormulaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.formula = extractJandexValue(
				annotation,
				HibernateAnnotations.JOIN_COLUMN_OR_FORMULA,
				"formula",
				modelContext
		);
		this.column = extractJandexValue(
				annotation,
				HibernateAnnotations.JOIN_COLUMN_OR_FORMULA,
				"column",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JoinColumnOrFormula.class;
	}

	@Override
	public org.hibernate.annotations.JoinFormula formula() {
		return formula;
	}

	public void formula(org.hibernate.annotations.JoinFormula value) {
		this.formula = value;
	}


	@Override
	public jakarta.persistence.JoinColumn column() {
		return column;
	}

	public void column(jakarta.persistence.JoinColumn value) {
		this.column = value;
	}


}
