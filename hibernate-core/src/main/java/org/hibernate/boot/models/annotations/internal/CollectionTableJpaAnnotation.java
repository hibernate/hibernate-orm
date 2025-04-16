/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionTableImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.CommonTableDetails;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.CollectionTable;

import static org.hibernate.boot.models.JpaAnnotations.COLLECTION_TABLE;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.applyCatalog;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.applyOptionalString;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.applySchema;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.collectIndexes;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.collectUniqueConstraints;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CollectionTableJpaAnnotation implements CollectionTable, CommonTableDetails {

	private String name;
	private String catalog;
	private String schema;
	private jakarta.persistence.JoinColumn[] joinColumns;
	private jakarta.persistence.ForeignKey foreignKey;
	private jakarta.persistence.UniqueConstraint[] uniqueConstraints;
	private jakarta.persistence.Index[] indexes;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CollectionTableJpaAnnotation(ModelsContext modelContext) {
		this.name = "";
		this.catalog = "";
		this.schema = "";
		this.joinColumns = new jakarta.persistence.JoinColumn[0];
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
		this.uniqueConstraints = new jakarta.persistence.UniqueConstraint[0];
		this.indexes = new jakarta.persistence.Index[0];
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CollectionTableJpaAnnotation(CollectionTable annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.catalog = annotation.catalog();
		this.schema = annotation.schema();
		this.joinColumns = extractJdkValue( annotation, COLLECTION_TABLE, "joinColumns", modelContext );
		this.foreignKey = extractJdkValue( annotation, COLLECTION_TABLE, "foreignKey", modelContext );
		this.uniqueConstraints = extractJdkValue( annotation, COLLECTION_TABLE, "uniqueConstraints", modelContext );
		this.indexes = extractJdkValue( annotation, COLLECTION_TABLE, "indexes", modelContext );
		this.options = extractJdkValue( annotation, COLLECTION_TABLE, "options", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CollectionTableJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.catalog = (String) attributeValues.get( "catalog" );
		this.schema = (String) attributeValues.get( "schema" );
		this.joinColumns = (jakarta.persistence.JoinColumn[]) attributeValues.get( "joinColumns" );
		this.foreignKey = (jakarta.persistence.ForeignKey) attributeValues.get( "foreignKey" );
		this.uniqueConstraints = (jakarta.persistence.UniqueConstraint[]) attributeValues.get( "uniqueConstraints" );
		this.indexes = (jakarta.persistence.Index[]) attributeValues.get( "indexes" );
		this.options = (String) attributeValues.get( "options" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CollectionTable.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String catalog() {
		return catalog;
	}

	public void catalog(String value) {
		this.catalog = value;
	}


	@Override
	public String schema() {
		return schema;
	}

	public void schema(String value) {
		this.schema = value;
	}


	@Override
	public jakarta.persistence.JoinColumn[] joinColumns() {
		return joinColumns;
	}

	public void joinColumns(jakarta.persistence.JoinColumn[] value) {
		this.joinColumns = value;
	}


	@Override
	public jakarta.persistence.ForeignKey foreignKey() {
		return foreignKey;
	}

	public void foreignKey(jakarta.persistence.ForeignKey value) {
		this.foreignKey = value;
	}


	@Override
	public jakarta.persistence.UniqueConstraint[] uniqueConstraints() {
		return uniqueConstraints;
	}

	public void uniqueConstraints(jakarta.persistence.UniqueConstraint[] value) {
		this.uniqueConstraints = value;
	}


	@Override
	public jakarta.persistence.Index[] indexes() {
		return indexes;
	}

	public void indexes(jakarta.persistence.Index[] value) {
		this.indexes = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}

	public void apply(JaxbCollectionTableImpl jaxbTable, XmlDocumentContext xmlDocumentContext) {
		applyOptionalString( jaxbTable.getName(), this::name );
		applyCatalog( jaxbTable, this, xmlDocumentContext );
		applySchema( jaxbTable, this, xmlDocumentContext );
		applyOptionalString( jaxbTable.getOptions(), this::options );

		joinColumns( JoinColumnProcessing.transformJoinColumnList( jaxbTable.getJoinColumns(), xmlDocumentContext ) );

		if ( jaxbTable.getForeignKeys() != null ) {
			foreignKey( ForeignKeyProcessing.createNestedForeignKeyAnnotation(
					jaxbTable.getForeignKeys(),
					xmlDocumentContext
			) );
		}

		indexes( collectIndexes( jaxbTable.getIndexes(), xmlDocumentContext ) );
		uniqueConstraints( collectUniqueConstraints( jaxbTable.getUniqueConstraints(), xmlDocumentContext ) );
	}


}
