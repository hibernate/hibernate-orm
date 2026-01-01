/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.PrimaryKeyJoinColumn;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrimaryKeyJoinColumnImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.ColumnDetails;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

import static org.hibernate.boot.models.JpaAnnotations.PRIMARY_KEY_JOIN_COLUMN;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.collectCheckConstraints;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class PrimaryKeyJoinColumnJpaAnnotation implements PrimaryKeyJoinColumn, ColumnDetails, ColumnDetails.Definable {
	private String name;
	private String referencedColumnName;
	private String comment;
	private String columnDefinition;
	private String options;
	private jakarta.persistence.ForeignKey foreignKey;
	private CheckConstraint[] check;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public PrimaryKeyJoinColumnJpaAnnotation(ModelsContext modelContext) {
		this.name = "";
		this.referencedColumnName = "";
		this.comment = "";
		this.columnDefinition = "";
		this.options = "";
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
		this.check = new CheckConstraint[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public PrimaryKeyJoinColumnJpaAnnotation(PrimaryKeyJoinColumn annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.referencedColumnName = annotation.referencedColumnName();
		this.comment = annotation.comment();
		this.columnDefinition = annotation.columnDefinition();
		this.options = annotation.options();
		this.foreignKey = extractJdkValue( annotation, PRIMARY_KEY_JOIN_COLUMN, "foreignKey", modelContext );
		this.check = extractJdkValue( annotation, PRIMARY_KEY_JOIN_COLUMN, "check", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public PrimaryKeyJoinColumnJpaAnnotation(
			Map<String, Object> attributeValues,
			ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.referencedColumnName = (String) attributeValues.get( "referencedColumnName" );
		this.comment = (String) attributeValues.get( "comment" );
		this.columnDefinition = (String) attributeValues.get( "columnDefinition" );
		this.options = (String) attributeValues.get( "options" );
		this.foreignKey = (jakarta.persistence.ForeignKey) attributeValues.get( "foreignKey" );
		this.check = (CheckConstraint[]) attributeValues.get( "check" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return PrimaryKeyJoinColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String referencedColumnName() {
		return referencedColumnName;
	}

	public void referencedColumnName(String value) {
		this.referencedColumnName = value;
	}


	@Override
	public String comment() {
		return comment;
	}

	public void comment(String comment) {
		this.comment = comment;
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
	public jakarta.persistence.ForeignKey foreignKey() {
		return foreignKey;
	}

	public void foreignKey(jakarta.persistence.ForeignKey value) {
		this.foreignKey = value;
	}


	@Override
	public CheckConstraint[] check() {
		return check;
	}

	public void check(CheckConstraint[] check) {
		this.check = check;
	}

	public void apply(JaxbPrimaryKeyJoinColumnImpl jaxbColumn, XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			name( jaxbColumn.getName() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getComment() ) ) {
			comment( jaxbColumn.getComment() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getColumnDefinition() ) ) {
			columnDefinition( jaxbColumn.getColumnDefinition() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getOptions() ) ) {
			options( jaxbColumn.getOptions() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getReferencedColumnName() ) ) {
			referencedColumnName( jaxbColumn.getReferencedColumnName() );
		}

		if ( jaxbColumn.getForeignKey() != null ) {
			foreignKey( ForeignKeyProcessing.createNestedForeignKeyAnnotation(
					jaxbColumn.getForeignKey(),
					xmlDocumentContext
			) );
		}

		check( collectCheckConstraints( jaxbColumn.getCheckConstraints(), xmlDocumentContext ) );
	}
}
