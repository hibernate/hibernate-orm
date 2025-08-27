/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;


import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class JoinColumnJpaAnnotation implements JoinColumn {
	private String name;
	private String referencedColumnName;
	private boolean unique;
	private boolean nullable;
	private boolean insertable;
	private boolean updatable;
	private String columnDefinition;
	private String options;
	private String table;
	private jakarta.persistence.ForeignKey foreignKey;
	private jakarta.persistence.CheckConstraint[] check;
	private String comment;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public JoinColumnJpaAnnotation(ModelsContext modelContext) {
		this.name = "";
		this.referencedColumnName = "";
		this.unique = false;
		this.nullable = true;
		this.insertable = true;
		this.updatable = true;
		this.columnDefinition = "";
		this.options = "";
		this.table = "";
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
		this.check = new jakarta.persistence.CheckConstraint[0];
		this.comment = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public JoinColumnJpaAnnotation(JoinColumn annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.referencedColumnName = annotation.referencedColumnName();
		this.unique = annotation.unique();
		this.nullable = annotation.nullable();
		this.insertable = annotation.insertable();
		this.updatable = annotation.updatable();
		this.columnDefinition = annotation.columnDefinition();
		this.options = annotation.options();
		this.table = annotation.table();
		this.foreignKey = extractJdkValue( annotation, JpaAnnotations.JOIN_COLUMN, "foreignKey", modelContext );
		this.check = extractJdkValue( annotation, JpaAnnotations.JOIN_COLUMN, "check", modelContext );
		this.comment = annotation.comment();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public JoinColumnJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.referencedColumnName = (String) attributeValues.get( "referencedColumnName" );
		this.unique = (boolean) attributeValues.get( "unique" );
		this.nullable = (boolean) attributeValues.get( "nullable" );
		this.insertable = (boolean) attributeValues.get( "insertable" );
		this.updatable = (boolean) attributeValues.get( "updatable" );
		this.columnDefinition = (String) attributeValues.get( "columnDefinition" );
		this.options = (String) attributeValues.get( "options" );
		this.table = (String) attributeValues.get( "table" );
		this.foreignKey = (jakarta.persistence.ForeignKey) attributeValues.get( "foreignKey" );
		this.check = (jakarta.persistence.CheckConstraint[]) attributeValues.get( "check" );
		this.comment = (String) attributeValues.get( "comment" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return JoinColumn.class;
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
	public jakarta.persistence.ForeignKey foreignKey() {
		return foreignKey;
	}

	public void foreignKey(jakarta.persistence.ForeignKey value) {
		this.foreignKey = value;
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


	public void apply(JaxbJoinColumnImpl jaxbColumn, XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			name( jaxbColumn.getName() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getTable() ) ) {
			table( jaxbColumn.getTable() );
		}

		if ( jaxbColumn.isNullable() != null ) {
			nullable( jaxbColumn.isNullable() );
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

		if ( jaxbColumn.isUnique() != null ) {
			unique( jaxbColumn.isUnique() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getComment() ) ) {
			comment( jaxbColumn.getComment() );
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

		check( XmlAnnotationHelper.collectCheckConstraints( jaxbColumn.getCheckConstraints(), xmlDocumentContext ) );
	}

	public void apply(JaxbColumnImpl jaxbColumn, XmlDocumentContext xmlDocumentContext) {
		// NOTE : used for handling <any/> mappings

		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			name( jaxbColumn.getName() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getTable() ) ) {
			table( jaxbColumn.getTable() );
		}

		if ( jaxbColumn.isNullable() != null ) {
			nullable( jaxbColumn.isNullable() );
		}

		if ( jaxbColumn.isInsertable() != null ) {
			insertable( jaxbColumn.isInsertable() );
		}
		if ( jaxbColumn.isInsertable() != null ) {
			updatable( jaxbColumn.isUpdatable() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getColumnDefinition() ) ) {
			columnDefinition( jaxbColumn.getColumnDefinition() );
		}
		if ( StringHelper.isNotEmpty( jaxbColumn.getOptions() ) ) {
			options( jaxbColumn.getOptions() );
		}

		if ( jaxbColumn.isUnique() != null ) {
			unique( jaxbColumn.isUnique() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getComment() ) ) {
			comment( jaxbColumn.getComment() );
		}

		check( XmlAnnotationHelper.collectCheckConstraints( jaxbColumn.getCheckConstraints(), xmlDocumentContext ) );
	}

	public static JoinColumn toJoinColumn(
			PrimaryKeyJoinColumn pkJoinColumn,
			ModelsContext ModelsContext) {
		final JoinColumnJpaAnnotation joinColumn = JpaAnnotations.JOIN_COLUMN.createUsage( ModelsContext );
		joinColumn.name( pkJoinColumn.name() );
		joinColumn.referencedColumnName( pkJoinColumn.referencedColumnName() );
		joinColumn.columnDefinition( pkJoinColumn.columnDefinition() );
		joinColumn.options( pkJoinColumn.options() );
		joinColumn.foreignKey( pkJoinColumn.foreignKey() );
		return joinColumn;
	}

	public static JoinColumn toJoinColumn(
			MapKeyJoinColumn mapKeyJoinColumn,
			ModelsContext ModelsContext) {
		final JoinColumnJpaAnnotation joinColumn = JpaAnnotations.JOIN_COLUMN.createUsage( ModelsContext );
		joinColumn.name( mapKeyJoinColumn.name() );
		joinColumn.table( mapKeyJoinColumn.table() );
		joinColumn.referencedColumnName( mapKeyJoinColumn.referencedColumnName() );
		joinColumn.nullable( mapKeyJoinColumn.nullable() );
		joinColumn.unique( mapKeyJoinColumn.unique() );
		joinColumn.insertable( mapKeyJoinColumn.insertable() );
		joinColumn.updatable( mapKeyJoinColumn.updatable() );
		joinColumn.columnDefinition( mapKeyJoinColumn.columnDefinition() );
		joinColumn.options( mapKeyJoinColumn.options() );
		joinColumn.foreignKey( mapKeyJoinColumn.foreignKey() );
		return joinColumn;
	}
}
