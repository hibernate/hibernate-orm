/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.JavaTypeRegistration;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JavaTypeRegistrationAnnotation implements JavaTypeRegistration {
	private java.lang.Class<?> javaType;
	private java.lang.Class<? extends org.hibernate.type.descriptor.java.BasicJavaType<?>> descriptorClass;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JavaTypeRegistrationAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JavaTypeRegistrationAnnotation(JavaTypeRegistration annotation, SourceModelBuildingContext modelContext) {
		this.javaType = annotation.javaType();
		this.descriptorClass = annotation.descriptorClass();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JavaTypeRegistrationAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext modelContext) {
		this.javaType = (Class<?>) attributeValues.get( "javaType" );
		this.descriptorClass = (Class<? extends org.hibernate.type.descriptor.java.BasicJavaType<?>>) attributeValues
				.get( "descriptorClass" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JavaTypeRegistration.class;
	}

	@Override
	public java.lang.Class<?> javaType() {
		return javaType;
	}

	public void javaType(java.lang.Class<?> value) {
		this.javaType = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.type.descriptor.java.BasicJavaType<?>> descriptorClass() {
		return descriptorClass;
	}

	public void descriptorClass(java.lang.Class<? extends org.hibernate.type.descriptor.java.BasicJavaType<?>> value) {
		this.descriptorClass = value;
	}


}
