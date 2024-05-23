/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AttributeDescriptor;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Array;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ArrayAnnotation implements Array {
	private int length;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ArrayAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ArrayAnnotation(Array annotation, SourceModelBuildingContext modelContext) {
		this.length = extractJdkValue( annotation, HibernateAnnotations.ARRAY, "length", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ArrayAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.length = extractJandexValue( annotation, HibernateAnnotations.ARRAY, "length", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Array.class;
	}

	@Override
	public int length() {
		return length;
	}

	public void length(int value) {
		this.length = value;
	}


}
