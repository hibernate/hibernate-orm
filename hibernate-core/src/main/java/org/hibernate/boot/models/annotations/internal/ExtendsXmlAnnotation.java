/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.internal.Extends;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ExtendsXmlAnnotation implements Extends {
	private String superType;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ExtendsXmlAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ExtendsXmlAnnotation(Extends annotation, SourceModelBuildingContext modelContext) {
		this.superType = annotation.superType();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ExtendsXmlAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.superType = (String) attributeValues.get( "superType" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Extends.class;
	}

	@Override
	public String superType() {
		return superType;
	}

	public void superType(String value) {
		this.superType = value;
	}


}
