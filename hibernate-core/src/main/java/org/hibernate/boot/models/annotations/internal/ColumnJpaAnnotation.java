/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.CheckConstraintCollector;
import org.hibernate.boot.models.annotations.spi.ColumnDetails;
import org.hibernate.boot.models.annotations.spi.Commentable;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.Column;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class ColumnJpaAnnotation implements Column,
		ColumnDetails.Nullable,
		ColumnDetails.Mutable,
		ColumnDetails.Sizable,
		ColumnDetails.Uniqueable,
		ColumnDetails.Definable,
		ColumnDetails.AlternateTableCapable,
		Commentable,
		CheckConstraintCollector {

	private String name;
	private boolean unique;
	private boolean nullable;
	private boolean insertable;
	private boolean updatable;
	private String columnDefinition;
	private String options;
	private String table;
	private int length;
	private int precision;
	private int scale;
	private int secondPrecision;
	private jakarta.persistence.CheckConstraint[] check;
	private String comment;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public ColumnJpaAnnotation(ModelsContext modelContext) {
		this.name = "";
		this.unique = false;
		this.nullable = true;
		this.insertable = true;
		this.updatable = true;
		this.columnDefinition = "";
		this.options = "";
		this.table = "";
		this.length = 255;
		this.precision = 0;
		this.scale = 0;
		this.secondPrecision = -1;
		this.check = new jakarta.persistence.CheckConstraint[0];
		this.comment = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public ColumnJpaAnnotation(Column annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.unique = annotation.unique();
		this.nullable = annotation.nullable();
		this.insertable = annotation.insertable();
		this.updatable = annotation.updatable();
		this.columnDefinition = annotation.columnDefinition();
		this.options = annotation.options();
		this.table = annotation.table();
		this.length = annotation.length();
		this.precision = annotation.precision();
		this.scale = annotation.scale();
		this.secondPrecision = annotation.secondPrecision();
		this.check = extractJdkValue( annotation, JpaAnnotations.COLUMN, "check", modelContext );
		this.comment = annotation.comment();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public ColumnJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.unique = (boolean) attributeValues.get( "unique" );
		this.nullable = (boolean) attributeValues.get( "nullable" );
		this.insertable = (boolean) attributeValues.get( "insertable" );
		this.updatable = (boolean) attributeValues.get( "updatable" );
		this.columnDefinition = (String) attributeValues.get( "columnDefinition" );
		this.options = (String) attributeValues.get( "options" );
		this.table = (String) attributeValues.get( "table" );
		this.length = (int) attributeValues.get( "length" );
		this.precision = (int) attributeValues.get( "precision" );
		this.scale = (int) attributeValues.get( "scale" );
		this.secondPrecision = (int) attributeValues.get( "secondPrecision" );
		this.check = (jakarta.persistence.CheckConstraint[]) attributeValues.get( "check" );
		this.comment = (String) attributeValues.get( "comment" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Column.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public boolean unique() {
		return unique;
	}

	public void unique(boolean value) {
		this.unique = value;
	}


	@Override
	public boolean nullable() {
		return nullable;
	}

	public void nullable(boolean value) {
		this.nullable = value;
	}


	@Override
	public boolean insertable() {
		return insertable;
	}

	public void insertable(boolean value) {
		this.insertable = value;
	}


	@Override
	public boolean updatable() {
		return updatable;
	}

	public void updatable(boolean value) {
		this.updatable = value;
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
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}


	@Override
	public int length() {
		return length;
	}

	public void length(int value) {
		this.length = value;
	}


	@Override
	public int precision() {
		return precision;
	}

	public void precision(int value) {
		this.precision = value;
	}


	@Override
	public int scale() {
		return scale;
	}

	public void scale(int value) {
		this.scale = value;
	}


	@Override
	public int secondPrecision() {
		return secondPrecision;
	}

	public void secondPrecision(int value) {
		this.secondPrecision = value;
	}


	@Override
	public jakarta.persistence.CheckConstraint[] check() {
		return check;
	}

	public void check(jakarta.persistence.CheckConstraint[] value) {
		this.check = value;
	}


	@Override
	public String comment() {
		return comment;
	}

	public void comment(String value) {
		this.comment = value;
	}

	public void apply(JaxbColumnImpl jaxbColumn, XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			name( jaxbColumn.getName() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getTable() ) ) {
			table( jaxbColumn.getTable() );
		}

		if ( jaxbColumn.isNullable() != null ) {
			nullable( jaxbColumn.isNullable() );
		}

		if ( jaxbColumn.isUnique() != null ) {
			unique( jaxbColumn.isUnique() );
		}

		if ( jaxbColumn.isInsertable() != null ) {
			insertable( jaxbColumn.isInsertable() );
		}

		if ( jaxbColumn.isUpdatable() != null ) {
			updatable( jaxbColumn.isUpdatable() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getColumnDefinition() ) ) {
			columnDefinition( jaxbColumn.getColumnDefinition() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getOptions() ) ) {
			options( jaxbColumn.getOptions() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getComment() ) ) {
			comment( jaxbColumn.getComment() );
		}

		if ( jaxbColumn.getLength() != null ) {
			length( jaxbColumn.getLength() );
		}

		if ( jaxbColumn.getPrecision() != null ) {
			precision( jaxbColumn.getPrecision() );
		}

		if ( jaxbColumn.getScale() != null ) {
			scale( jaxbColumn.getScale() );
		}

		if ( jaxbColumn.getSecondPrecision() != null ) {
			secondPrecision( jaxbColumn.getSecondPrecision() );
		}

		check( XmlAnnotationHelper.collectCheckConstraints( jaxbColumn.getCheckConstraints(), xmlDocumentContext ) );
	}

}
