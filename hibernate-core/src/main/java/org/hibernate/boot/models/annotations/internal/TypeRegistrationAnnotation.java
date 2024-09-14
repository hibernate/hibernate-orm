/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.TypeRegistration;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TypeRegistrationAnnotation implements TypeRegistration {
	private java.lang.Class<?> basicClass;
	private java.lang.Class<? extends org.hibernate.usertype.UserType<?>> userType;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TypeRegistrationAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TypeRegistrationAnnotation(TypeRegistration annotation, SourceModelBuildingContext modelContext) {
		this.basicClass = annotation.basicClass();
		this.userType = annotation.userType();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TypeRegistrationAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.basicClass = (Class<?>) attributeValues.get( "basicClass" );
		this.userType = (Class<? extends org.hibernate.usertype.UserType<?>>) attributeValues.get( "userType" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TypeRegistration.class;
	}

	@Override
	public java.lang.Class<?> basicClass() {
		return basicClass;
	}

	public void basicClass(java.lang.Class<?> value) {
		this.basicClass = value;
	}


	@Override
	public java.lang.Class<? extends org.hibernate.usertype.UserType<?>> userType() {
		return userType;
	}

	public void userType(java.lang.Class<? extends org.hibernate.usertype.UserType<?>> value) {
		this.userType = value;
	}


}
