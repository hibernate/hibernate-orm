/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class LazyCollectionAnnotation implements LazyCollection {
	private org.hibernate.annotations.LazyCollectionOption value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public LazyCollectionAnnotation(SourceModelBuildingContext modelContext) {
		this.value = org.hibernate.annotations.LazyCollectionOption.TRUE;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public LazyCollectionAnnotation(LazyCollection annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.LAZY_COLLECTION, "value", modelContext );
	}


	@Override
	public Class<? extends Annotation> annotationType() {
		return LazyCollection.class;
	}

	@Override
	public org.hibernate.annotations.LazyCollectionOption value() {
		return value;
	}

	public void value(org.hibernate.annotations.LazyCollectionOption value) {
		this.value = value;
	}


}
