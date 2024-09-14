/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class AttributeBinderTypeAnnotation implements AttributeBinderType {
	private java.lang.Class<? extends org.hibernate.binder.AttributeBinder<?>> binder;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public AttributeBinderTypeAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public AttributeBinderTypeAnnotation(AttributeBinderType annotation, SourceModelBuildingContext modelContext) {
		this.binder = annotation.binder();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public AttributeBinderTypeAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.binder = (Class<? extends org.hibernate.binder.AttributeBinder<?>>) attributeValues.get( "binder" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return AttributeBinderType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.binder.AttributeBinder<?>> binder() {
		return binder;
	}

	public void binder(java.lang.Class<? extends org.hibernate.binder.AttributeBinder<?>> value) {
		this.binder = value;
	}


}
