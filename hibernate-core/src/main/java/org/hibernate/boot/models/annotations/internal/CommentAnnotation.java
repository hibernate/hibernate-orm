/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Comment;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CommentAnnotation implements Comment {
	private String value;
	private String on;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CommentAnnotation(ModelsContext modelContext) {
		this.on = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CommentAnnotation(Comment annotation, ModelsContext modelContext) {
		this.value = annotation.value();
		this.on = annotation.on();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CommentAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.value = (String) attributeValues.get( "value" );
		this.on = (String) attributeValues.get( "on" );
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
