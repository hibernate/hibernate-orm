/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.AttributeOverride;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AttributeOverrideJpaAnnotation implements AttributeOverride {

	private String name;
	private jakarta.persistence.Column column;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AttributeOverrideJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AttributeOverrideJpaAnnotation(AttributeOverride annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.column = extractJdkValue( annotation, JpaAnnotations.ATTRIBUTE_OVERRIDE, "column", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AttributeOverrideJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.column = (jakarta.persistence.Column) attributeValues.get( "column" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AttributeOverride.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public jakarta.persistence.Column column() {
		return column;
	}

	public void column(jakarta.persistence.Column value) {
		this.column = value;
	}


}
