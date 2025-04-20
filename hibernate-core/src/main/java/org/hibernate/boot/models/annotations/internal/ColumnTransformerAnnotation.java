/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ColumnTransformerAnnotation implements ColumnTransformer {
	private String forColumn;
	private String read;
	private String write;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ColumnTransformerAnnotation(ModelsContext modelContext) {
		this.forColumn = "";
		this.read = "";
		this.write = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ColumnTransformerAnnotation(ColumnTransformer annotation, ModelsContext modelContext) {
		this.forColumn = annotation.forColumn();
		this.read = annotation.read();
		this.write = annotation.write();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ColumnTransformerAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.forColumn = (String) attributeValues.get( "forColumn" );
		this.read = (String) attributeValues.get( "read" );
		this.write = (String) attributeValues.get( "write" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return ColumnTransformer.class;
	}

	@Override
	public String forColumn() {
		return forColumn;
	}

	public void forColumn(String value) {
		this.forColumn = value;
	}


	@Override
	public String read() {
		return read;
	}

	public void read(String value) {
		this.read = value;
	}


	@Override
	public String write() {
		return write;
	}

	public void write(String value) {
		this.write = value;
	}


}
