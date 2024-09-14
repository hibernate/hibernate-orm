/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Struct;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class StructAnnotation implements Struct {
	private String name;
	private String catalog;
	private String schema;
	private String[] attributes;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public StructAnnotation(SourceModelBuildingContext modelContext) {
		this.attributes = new String[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public StructAnnotation(Struct annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.catalog = annotation.catalog();
		this.schema = annotation.schema();
		this.attributes = annotation.attributes();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public StructAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.catalog = (String) attributeValues.get( "catalog" );
		this.schema = (String) attributeValues.get( "schema" );
		this.attributes = (String[]) attributeValues.get( "attributes" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Struct.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
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
	public String[] attributes() {
		return attributes;
	}

	public void attributes(String[] value) {
		this.attributes = value;
	}


}
