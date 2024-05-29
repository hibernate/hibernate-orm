/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.CollectionType;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.HibernateAnnotations.COLLECTION_TYPE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CollectionTypeAnnotation implements CollectionType {
	private java.lang.Class<? extends org.hibernate.usertype.UserCollectionType> type;
	private org.hibernate.annotations.Parameter[] parameters;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionTypeAnnotation(SourceModelBuildingContext modelContext) {
		this.parameters = new org.hibernate.annotations.Parameter[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionTypeAnnotation(CollectionType annotation, SourceModelBuildingContext modelContext) {
		this.type = annotation.type();
		this.parameters = extractJdkValue( annotation, COLLECTION_TYPE, "parameters", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionTypeAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.type = extractJandexValue( annotation, COLLECTION_TYPE, "type", modelContext );
		this.parameters = extractJandexValue(
				annotation,
				COLLECTION_TYPE,
				"parameters",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionType.class;
	}

	@Override
	public java.lang.Class<? extends org.hibernate.usertype.UserCollectionType> type() {
		return type;
	}

	public void type(java.lang.Class<? extends org.hibernate.usertype.UserCollectionType> value) {
		this.type = value;
	}


	@Override
	public org.hibernate.annotations.Parameter[] parameters() {
		return parameters;
	}

	public void parameters(org.hibernate.annotations.Parameter[] value) {
		this.parameters = value;
	}


}
