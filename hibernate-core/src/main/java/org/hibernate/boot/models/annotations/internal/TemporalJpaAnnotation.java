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

import jakarta.persistence.Temporal;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TemporalJpaAnnotation implements Temporal {
	private jakarta.persistence.TemporalType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TemporalJpaAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TemporalJpaAnnotation(Temporal annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TemporalJpaAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (jakarta.persistence.TemporalType) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Temporal.class;
	}

	@Override
	public jakarta.persistence.TemporalType value() {
		return value;
	}

	public void value(jakarta.persistence.TemporalType value) {
		this.value = value;
	}


}
