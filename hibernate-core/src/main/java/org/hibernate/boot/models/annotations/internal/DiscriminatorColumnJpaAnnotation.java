/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorColumnImpl;
import org.hibernate.boot.models.annotations.spi.ColumnDetails;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.DiscriminatorColumn;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

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
	public DiscriminatorColumnJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "DTYPE";
		this.discriminatorType = jakarta.persistence.DiscriminatorType.STRING;
		this.columnDefinition = "";
		this.options = "";
		this.length = 31;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public DiscriminatorColumnJpaAnnotation(DiscriminatorColumn annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.discriminatorType = annotation.discriminatorType();
		this.columnDefinition = annotation.columnDefinition();
		this.options = annotation.options();
		this.length = annotation.length();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public DiscriminatorColumnJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue(
				annotation,
				org.hibernate.boot.models.JpaAnnotations.DISCRIMINATOR_COLUMN,
				"name",
				modelContext
		);
		this.discriminatorType = extractJandexValue(
				annotation,
				org.hibernate.boot.models.JpaAnnotations.DISCRIMINATOR_COLUMN,
				"discriminatorType",
				modelContext
		);
		this.columnDefinition = extractJandexValue(
				annotation,
				org.hibernate.boot.models.JpaAnnotations.DISCRIMINATOR_COLUMN,
				"columnDefinition",
				modelContext
		);
		this.options = extractJandexValue(
				annotation,
				org.hibernate.boot.models.JpaAnnotations.DISCRIMINATOR_COLUMN,
				"options",
				modelContext
		);
		this.length = extractJandexValue(
				annotation,
				org.hibernate.boot.models.JpaAnnotations.DISCRIMINATOR_COLUMN,
				"length",
				modelContext
		);
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
