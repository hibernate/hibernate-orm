/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.OnDelete;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OnDeleteAnnotation implements OnDelete {
	private org.hibernate.annotations.OnDeleteAction action;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OnDeleteAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OnDeleteAnnotation(OnDelete annotation, SourceModelBuildingContext modelContext) {
		this.action = annotation.action();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OnDeleteAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.action = extractJandexValue( annotation, HibernateAnnotations.ON_DELETE, "action", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return OnDelete.class;
	}

	@Override
	public org.hibernate.annotations.OnDeleteAction action() {
		return action;
	}

	public void action(org.hibernate.annotations.OnDeleteAction value) {
		this.action = value;
	}


}
