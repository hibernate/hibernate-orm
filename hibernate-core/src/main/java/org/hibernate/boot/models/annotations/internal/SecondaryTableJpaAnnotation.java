/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbSecondaryTableImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.CommonTableDetails;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.internal.db.JoinColumnProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.SecondaryTable;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.applyOptionalString;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.collectCheckConstraints;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.collectIndexes;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.collectUniqueConstraints;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class SecondaryTableJpaAnnotation implements SecondaryTable, CommonTableDetails {
	private String name;
	private String catalog;
	private String schema;
	private jakarta.persistence.PrimaryKeyJoinColumn[] pkJoinColumns;
	private jakarta.persistence.ForeignKey foreignKey;
	private jakarta.persistence.UniqueConstraint[] uniqueConstraints;
	private jakarta.persistence.Index[] indexes;
	private jakarta.persistence.CheckConstraint[] check;
	private String comment;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public SecondaryTableJpaAnnotation(ModelsContext modelContext) {
		this.catalog = "";
		this.schema = "";
		this.pkJoinColumns = new jakarta.persistence.PrimaryKeyJoinColumn[0];
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
		this.uniqueConstraints = new jakarta.persistence.UniqueConstraint[0];
		this.indexes = new jakarta.persistence.Index[0];
		this.check = new jakarta.persistence.CheckConstraint[0];
		this.comment = "";
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public SecondaryTableJpaAnnotation(SecondaryTable annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.catalog = annotation.catalog();
		this.schema = annotation.schema();
		this.pkJoinColumns = extractJdkValue(
				annotation,
				JpaAnnotations.SECONDARY_TABLE,
				"pkJoinColumns",
				modelContext
		);
		this.foreignKey = extractJdkValue( annotation, JpaAnnotations.SECONDARY_TABLE, "foreignKey", modelContext );
		this.uniqueConstraints = extractJdkValue(
				annotation,
				JpaAnnotations.SECONDARY_TABLE,
				"uniqueConstraints",
				modelContext
		);
		this.indexes = extractJdkValue( annotation, JpaAnnotations.SECONDARY_TABLE, "indexes", modelContext );
		this.check = extractJdkValue( annotation, JpaAnnotations.SECONDARY_TABLE, "check", modelContext );
		this.comment = annotation.comment();
		this.options = annotation.options();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public SecondaryTableJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.catalog = (String) attributeValues.get( "catalog" );
		this.schema = (String) attributeValues.get( "schema" );
		this.pkJoinColumns = (jakarta.persistence.PrimaryKeyJoinColumn[]) attributeValues.get( "pkJoinColumns" );
		this.foreignKey = (jakarta.persistence.ForeignKey) attributeValues.get( "foreignKey" );
		this.uniqueConstraints = (jakarta.persistence.UniqueConstraint[]) attributeValues.get( "uniqueConstraints" );
		this.indexes = (jakarta.persistence.Index[]) attributeValues.get( "indexes" );
		this.check = (jakarta.persistence.CheckConstraint[]) attributeValues.get( "check" );
		this.comment = (String) attributeValues.get( "comment" );
		this.options = (String) attributeValues.get( "options" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return SecondaryTable.class;
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
	public jakarta.persistence.PrimaryKeyJoinColumn[] pkJoinColumns() {
		return pkJoinColumns;
	}

	public void pkJoinColumns(jakarta.persistence.PrimaryKeyJoinColumn[] value) {
		this.pkJoinColumns = value;
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


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


	public void apply(JaxbSecondaryTableImpl jaxbTable, XmlDocumentContext xmlDocumentContext) {
		name( jaxbTable.getName() );
		applyOptionalString( jaxbTable.getCatalog(), this::catalog );
		applyOptionalString( jaxbTable.getSchema(), this::schema );
		applyOptionalString( jaxbTable.getComment(), this::comment );
		applyOptionalString( jaxbTable.getOptions(), this::options );

		check( collectCheckConstraints( jaxbTable.getCheckConstraints(), xmlDocumentContext ) );
		indexes( collectIndexes( jaxbTable.getIndexes(), xmlDocumentContext ) );
		uniqueConstraints( collectUniqueConstraints( jaxbTable.getUniqueConstraints(), xmlDocumentContext ) );

		pkJoinColumns( JoinColumnProcessing.transformPrimaryKeyJoinColumns(
				jaxbTable.getPrimaryKeyJoinColumn(),
				xmlDocumentContext
		) );

		if ( jaxbTable.getForeignKey() != null ) {
			foreignKey( ForeignKeyProcessing.createNestedForeignKeyAnnotation(
					jaxbTable.getForeignKey(),
					xmlDocumentContext
			) );
		}
	}
}
