/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class DiscriminatorOptionsAnnotation implements DiscriminatorOptions {
	private boolean force;
	private boolean insert;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public DiscriminatorOptionsAnnotation(SourceModelBuildingContext modelContext) {
		this.force = false;
		this.insert = true;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public DiscriminatorOptionsAnnotation(DiscriminatorOptions annotation, SourceModelBuildingContext modelContext) {
		this.force = annotation.force();
		this.insert = annotation.insert();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public DiscriminatorOptionsAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.force = extractJandexValue(
				annotation,
				HibernateAnnotations.DISCRIMINATOR_OPTIONS,
				"force",
				modelContext
		);
		this.insert = extractJandexValue(
				annotation,
				HibernateAnnotations.DISCRIMINATOR_OPTIONS,
				"insert",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DiscriminatorOptions.class;
	}

	@Override
	public boolean force() {
		return force;
	}

	public void force(boolean value) {
		this.force = value;
	}


	@Override
	public boolean insert() {
		return insert;
	}

	public void insert(boolean value) {
		this.insert = value;
	}


}
