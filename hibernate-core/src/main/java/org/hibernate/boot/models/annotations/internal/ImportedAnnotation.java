/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Imported;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ImportedAnnotation implements Imported {
	private String rename;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ImportedAnnotation(SourceModelBuildingContext modelContext) {
		this.rename = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ImportedAnnotation(Imported annotation, SourceModelBuildingContext modelContext) {
		this.rename = annotation.rename();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ImportedAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.rename = (String) attributeValues.get( "rename" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Imported.class;
	}

	@Override
	public String rename() {
		return rename;
	}

	public void rename(String value) {
		this.rename = value;
	}


}
