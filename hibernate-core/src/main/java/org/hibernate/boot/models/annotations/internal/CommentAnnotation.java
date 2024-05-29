/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.Comment;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CommentAnnotation implements Comment {
	private String value;
	private String on;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CommentAnnotation(SourceModelBuildingContext modelContext) {
		this.on = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CommentAnnotation(Comment annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
		this.on = annotation.on();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CommentAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJandexValue( annotation, HibernateAnnotations.COMMENT, "value", modelContext );
		this.on = extractJandexValue( annotation, HibernateAnnotations.COMMENT, "on", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Comment.class;
	}

	@Override
	public String value() {
		return value;
	}

	public void value(String value) {
		this.value = value;
	}


	@Override
	public String on() {
		return on;
	}

	public void on(String value) {
		this.on = value;
	}


}
