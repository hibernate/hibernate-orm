/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.HibernateAnnotations.JOIN_COLUMN_OR_FORMULA;
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
		this.formula = HibernateAnnotations.JOIN_FORMULA.createUsage( modelContext );
		this.column = JpaAnnotations.JOIN_COLUMN.createUsage( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JoinColumnOrFormulaAnnotation(JoinColumnOrFormula annotation, SourceModelBuildingContext modelContext) {
		this.formula = extractJdkValue( annotation, JOIN_COLUMN_OR_FORMULA, "formula", modelContext );
		this.column = extractJdkValue( annotation, JOIN_COLUMN_OR_FORMULA, "column", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JoinColumnOrFormulaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.formula = (org.hibernate.annotations.JoinFormula) attributeValues.get( "formula" );
		this.column = (jakarta.persistence.JoinColumn) attributeValues.get( "column" );
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
