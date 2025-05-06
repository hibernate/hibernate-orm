/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorColumnImpl;
import org.hibernate.boot.models.annotations.spi.ColumnDetails;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.DiscriminatorColumn;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class DiscriminatorColumnJpaAnnotation implements DiscriminatorColumn, ColumnDetails {

	private String name;
	private jakarta.persistence.DiscriminatorType discriminatorType;
	private String columnDefinition;
	private String options;
	private int length;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public DiscriminatorColumnJpaAnnotation(ModelsContext modelContext) {
		this.name = "DTYPE";
		this.discriminatorType = jakarta.persistence.DiscriminatorType.STRING;
		this.columnDefinition = "";
		this.options = "";
		this.length = 31;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public DiscriminatorColumnJpaAnnotation(DiscriminatorColumn annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.discriminatorType = annotation.discriminatorType();
		this.columnDefinition = annotation.columnDefinition();
		this.options = annotation.options();
		this.length = annotation.length();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public DiscriminatorColumnJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.discriminatorType = (jakarta.persistence.DiscriminatorType) attributeValues.get( "discriminatorType" );
		this.columnDefinition = (String) attributeValues.get( "columnDefinition" );
		this.options = (String) attributeValues.get( "options" );
		this.length = (int) attributeValues.get( "length" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DiscriminatorColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public jakarta.persistence.DiscriminatorType discriminatorType() {
		return discriminatorType;
	}

	public void discriminatorType(jakarta.persistence.DiscriminatorType value) {
		this.discriminatorType = value;
	}


	@Override
	public String columnDefinition() {
		return columnDefinition;
	}

	public void columnDefinition(String value) {
		this.columnDefinition = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


	@Override
	public int length() {
		return length;
	}

	public void length(int value) {
		this.length = value;
	}


	public void apply(JaxbDiscriminatorColumnImpl jaxbColumn, XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			name( jaxbColumn.getName() );
		}

		if ( jaxbColumn.getDiscriminatorType() != null ) {
			discriminatorType( jaxbColumn.getDiscriminatorType() );
		}

		if ( jaxbColumn.getLength() != null ) {
			length( jaxbColumn.getLength() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getColumnDefinition() ) ) {
			columnDefinition( jaxbColumn.getColumnDefinition() );
		}
		if ( StringHelper.isNotEmpty( jaxbColumn.getOptions() ) ) {
			options( jaxbColumn.getOptions() );
		}
	}
}
