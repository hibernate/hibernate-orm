/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.models.spi.SourceModelBuildingContext;



@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AttributeAccessorAnnotation implements AttributeAccessor {
	private String value;
	private java.lang.Class<? extends org.hibernate.property.access.spi.PropertyAccessStrategy> strategy;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AttributeAccessorAnnotation(SourceModelBuildingContext modelContext) {
		this.value = "";
		this.strategy = org.hibernate.property.access.spi.PropertyAccessStrategy.class;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AttributeAccessorAnnotation(AttributeAccessor annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
		this.strategy = annotation.strategy();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AttributeAccessorAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (String) attributeValues.get( "value" );
		this.strategy = (Class<? extends org.hibernate.property.access.spi.PropertyAccessStrategy>) attributeValues
				.get( "strategy" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AttributeAccessor.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.property.access.spi.PropertyAccessStrategy> strategy() {
		return strategy;
	}

	public void strategy(java.lang.Class<? extends org.hibernate.property.access.spi.PropertyAccessStrategy> value) {
		this.strategy = value;
	}


}
