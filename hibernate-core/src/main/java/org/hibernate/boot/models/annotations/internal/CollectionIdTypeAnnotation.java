/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.CollectionIdType;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.HibernateAnnotations.COLLECTION_ID_TYPE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CollectionIdTypeAnnotation implements CollectionIdType {
	private java.lang.Class<? extends org.hibernate.usertype.UserType<?>> value;
	private org.hibernate.annotations.Parameter[] parameters;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionIdTypeAnnotation(SourceModelBuildingContext modelContext) {
		this.parameters = new org.hibernate.annotations.Parameter[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionIdTypeAnnotation(CollectionIdType annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
		this.parameters = extractJdkValue( annotation, COLLECTION_ID_TYPE, "parameters", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionIdTypeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, COLLECTION_ID_TYPE, "value", modelContext );
		this.parameters = extractJandexValue(
				annotation,
				COLLECTION_ID_TYPE,
				"parameters",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionIdType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.usertype.UserType<?>> value() {
		return value;
	}

	public void value(java.lang.Class<? extends org.hibernate.usertype.UserType<?>> value) {
		this.value = value;
	}


	@Override
	public org.hibernate.annotations.Parameter[] parameters() {
		return parameters;
	}

	public void parameters(org.hibernate.annotations.Parameter[] value) {
		this.parameters = value;
	}


}
