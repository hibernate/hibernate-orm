/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.MapKey;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyJpaAnnotation implements MapKey {
	private String name;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyJpaAnnotation(MapKey annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKey.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


}
