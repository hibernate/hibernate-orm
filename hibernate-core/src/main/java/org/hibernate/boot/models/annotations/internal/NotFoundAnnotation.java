/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.NotFound;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NotFoundAnnotation implements NotFound {
	private org.hibernate.annotations.NotFoundAction action;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NotFoundAnnotation(SourceModelBuildingContext modelContext) {
		this.action = org.hibernate.annotations.NotFoundAction.EXCEPTION;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NotFoundAnnotation(NotFound annotation, SourceModelBuildingContext modelContext) {
		this.action = extractJdkValue( annotation, HibernateAnnotations.NOT_FOUND, "action", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NotFoundAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.action = extractJandexValue( annotation, HibernateAnnotations.NOT_FOUND, "action", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NotFound.class;
	}

	@Override
	public org.hibernate.annotations.NotFoundAction action() {
		return action;
	}

	public void action(org.hibernate.annotations.NotFoundAction value) {
		this.action = value;
	}


}
