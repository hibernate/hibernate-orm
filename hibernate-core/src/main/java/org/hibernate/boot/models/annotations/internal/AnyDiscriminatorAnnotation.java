/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AnyDiscriminatorAnnotation implements AnyDiscriminator {
	private jakarta.persistence.DiscriminatorType value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AnyDiscriminatorAnnotation(SourceModelBuildingContext modelContext) {
		this.value = jakarta.persistence.DiscriminatorType.STRING;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AnyDiscriminatorAnnotation(AnyDiscriminator annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AnyDiscriminatorAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (jakarta.persistence.DiscriminatorType) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AnyDiscriminator.class;
	}

	@Override
	public jakarta.persistence.DiscriminatorType value() {
		return value;
	}

	public void value(jakarta.persistence.DiscriminatorType value) {
		this.value = value;
	}


}
