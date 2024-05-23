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

import jakarta.persistence.SequenceGenerator;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SequenceGeneratorJpaAnnotation implements SequenceGenerator {
	private String name;
	private String sequenceName;
	private String catalog;
	private String schema;
	private int initialValue;
	private int allocationSize;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SequenceGeneratorJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.sequenceName = "";
		this.catalog = "";
		this.schema = "";
		this.initialValue = 1;
		this.allocationSize = 50;
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SequenceGeneratorJpaAnnotation(SequenceGenerator annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, JpaAnnotations.SEQUENCE_GENERATOR, "name", modelContext );
		this.sequenceName = extractJdkValue(
				annotation,
				JpaAnnotations.SEQUENCE_GENERATOR,
				"sequenceName",
				modelContext
		);
		this.catalog = extractJdkValue( annotation, JpaAnnotations.SEQUENCE_GENERATOR, "catalog", modelContext );
		this.schema = extractJdkValue( annotation, JpaAnnotations.SEQUENCE_GENERATOR, "schema", modelContext );
		this.initialValue = extractJdkValue(
				annotation,
				JpaAnnotations.SEQUENCE_GENERATOR,
				"initialValue",
				modelContext
		);
		this.allocationSize = extractJdkValue(
				annotation,
				JpaAnnotations.SEQUENCE_GENERATOR,
				"allocationSize",
				modelContext
		);
		this.options = extractJdkValue( annotation, JpaAnnotations.SEQUENCE_GENERATOR, "options", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SequenceGeneratorJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.SEQUENCE_GENERATOR, "name", modelContext );
		this.sequenceName = extractJandexValue(
				annotation,
				JpaAnnotations.SEQUENCE_GENERATOR,
				"sequenceName",
				modelContext
		);
		this.catalog = extractJandexValue( annotation, JpaAnnotations.SEQUENCE_GENERATOR, "catalog", modelContext );
		this.schema = extractJandexValue( annotation, JpaAnnotations.SEQUENCE_GENERATOR, "schema", modelContext );
		this.initialValue = extractJandexValue(
				annotation,
				JpaAnnotations.SEQUENCE_GENERATOR,
				"initialValue",
				modelContext
		);
		this.allocationSize = extractJandexValue(
				annotation,
				JpaAnnotations.SEQUENCE_GENERATOR,
				"allocationSize",
				modelContext
		);
		this.options = extractJandexValue( annotation, JpaAnnotations.SEQUENCE_GENERATOR, "options", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SequenceGenerator.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String sequenceName() {
		return sequenceName;
	}

	public void sequenceName(String value) {
		this.sequenceName = value;
	}


	@Override
	public String catalog() {
		return catalog;
	}

	public void catalog(String value) {
		this.catalog = value;
	}


	@Override
	public String schema() {
		return schema;
	}

	public void schema(String value) {
		this.schema = value;
	}


	@Override
	public int initialValue() {
		return initialValue;
	}

	public void initialValue(int value) {
		this.initialValue = value;
	}


	@Override
	public int allocationSize() {
		return allocationSize;
	}

	public void allocationSize(int value) {
		this.allocationSize = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


}
