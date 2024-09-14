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

import jakarta.persistence.Cacheable;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CacheableJpaAnnotation implements Cacheable {
	private boolean value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CacheableJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.value = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CacheableJpaAnnotation(Cacheable annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CacheableJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (boolean) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Cacheable.class;
	}

	@Override
	public boolean value() {
		return value;
	}

	public void value(boolean value) {
		this.value = value;
	}


}
