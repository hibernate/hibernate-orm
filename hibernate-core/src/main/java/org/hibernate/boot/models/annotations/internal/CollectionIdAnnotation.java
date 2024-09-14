/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CollectionId;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CollectionIdAnnotation implements CollectionId {
	private jakarta.persistence.Column column;
	private java.lang.Class<? extends org.hibernate.id.IdentifierGenerator> generatorImplementation;
	private String generator;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionIdAnnotation(SourceModelBuildingContext modelContext) {
		this.column = JpaAnnotations.COLUMN.createUsage( modelContext );
		this.generatorImplementation = org.hibernate.id.IdentifierGenerator.class;
		this.generator = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionIdAnnotation(CollectionId annotation, SourceModelBuildingContext modelContext) {
		this.column = extractJdkValue( annotation, HibernateAnnotations.COLLECTION_ID, "column", modelContext );
		this.generatorImplementation = annotation.generatorImplementation();
		this.generator = annotation.generator();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionIdAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.column = (jakarta.persistence.Column) attributeValues.get( "column" );
		this.generatorImplementation = (Class<? extends org.hibernate.id.IdentifierGenerator>) attributeValues
				.get( "generatorImplementation" );
		this.generator = (String) attributeValues.get( "generator" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionId.class;
	}

	@Override
	public jakarta.persistence.Column column() {
		return column;
	}

	public void column(jakarta.persistence.Column value) {
		this.column = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.id.IdentifierGenerator> generatorImplementation() {
		return generatorImplementation;
	}

	public void generatorImplementation(java.lang.Class<? extends org.hibernate.id.IdentifierGenerator> value) {
		this.generatorImplementation = value;
	}


	@Override
	public String generator() {
		return generator;
	}

	public void generator(String value) {
		this.generator = value;
	}


}
