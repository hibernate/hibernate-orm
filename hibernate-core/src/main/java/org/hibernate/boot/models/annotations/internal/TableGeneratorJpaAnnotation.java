/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.models.annotations.spi.IndexCollector;
import org.hibernate.boot.models.annotations.spi.UniqueConstraintCollector;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.TableGenerator;

import static org.hibernate.boot.models.JpaAnnotations.TABLE_GENERATOR;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.collectIndexes;
import static org.hibernate.boot.models.xml.internal.XmlAnnotationHelper.collectUniqueConstraints;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class TableGeneratorJpaAnnotation implements TableGenerator, UniqueConstraintCollector, IndexCollector {
	private String name;
	private String table;
	private String catalog;
	private String schema;
	private String pkColumnName;
	private String valueColumnName;
	private String pkColumnValue;
	private int initialValue;
	private int allocationSize;
	private jakarta.persistence.UniqueConstraint[] uniqueConstraints;
	private jakarta.persistence.Index[] indexes;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public TableGeneratorJpaAnnotation(ModelsContext modelContext) {
		this( "", modelContext );
	}

	/**
	 * Used in creating named, defaulted annotation instances.  Generally this
	 * is a situation where we have:<ol>
	 *     <li>{@linkplain jakarta.persistence.GeneratedValue#strategy()} set to {@linkplain jakarta.persistence.GenerationType#TABLE}</li>
	 *     <li>{@linkplain jakarta.persistence.GeneratedValue#generator()} set to a non-empty String, but with no matching {@linkplain TableGenerator}</li>
	 * </ol>
	 */
	public TableGeneratorJpaAnnotation(String name, ModelsContext modelContext) {
		this.name = name;
		this.table = "";
		this.catalog = "";
		this.schema = "";
		this.pkColumnName = "";
		this.valueColumnName = "";
		this.pkColumnValue = "";
		this.initialValue = 0;
		this.allocationSize = 50;
		this.uniqueConstraints = new jakarta.persistence.UniqueConstraint[0];
		this.indexes = new jakarta.persistence.Index[0];
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public TableGeneratorJpaAnnotation(TableGenerator annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.table = annotation.table();
		this.catalog = annotation.catalog();
		this.schema = annotation.schema();
		this.pkColumnName = annotation.pkColumnName();
		this.valueColumnName = annotation.valueColumnName();
		this.pkColumnValue = annotation.pkColumnValue();
		this.initialValue = annotation.initialValue();
		this.allocationSize = annotation.allocationSize();
		this.uniqueConstraints = extractJdkValue( annotation, TABLE_GENERATOR, "uniqueConstraints", modelContext );
		this.indexes = extractJdkValue( annotation, TABLE_GENERATOR, "indexes", modelContext );
		this.options = annotation.options();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TableGeneratorJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.table = (String) attributeValues.get( "table" );
		this.catalog = (String) attributeValues.get( "catalog" );
		this.schema = (String) attributeValues.get( "schema" );
		this.pkColumnName = (String) attributeValues.get( "pkColumnName" );
		this.valueColumnName = (String) attributeValues.get( "valueColumnName" );
		this.pkColumnValue = (String) attributeValues.get( "pkColumnValue" );
		this.initialValue = (int) attributeValues.get( "initialValue" );
		this.allocationSize = (int) attributeValues.get( "allocationSize" );
		this.uniqueConstraints = (jakarta.persistence.UniqueConstraint[]) attributeValues.get( "uniqueConstraints" );
		this.indexes = (jakarta.persistence.Index[]) attributeValues.get( "indexes" );
		this.options = (String) attributeValues.get( "options" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return TableGenerator.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
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
	public String pkColumnName() {
		return pkColumnName;
	}

	public void pkColumnName(String value) {
		this.pkColumnName = value;
	}


	@Override
	public String valueColumnName() {
		return valueColumnName;
	}

	public void valueColumnName(String value) {
		this.valueColumnName = value;
	}


	@Override
	public String pkColumnValue() {
		return pkColumnValue;
	}

	public void pkColumnValue(String value) {
		this.pkColumnValue = value;
	}


	@Override
	public int initialValue() {
		return initialValue;
	}

	public void initialValue(int value) {
		this.initialValue = value;
	}


	@Override
	public int allocationSize() {
		return allocationSize;
	}

	public void allocationSize(int value) {
		this.allocationSize = value;
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


	public void apply(JaxbTableGeneratorImpl jaxbGenerator, XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbGenerator.getName() ) ) {
			name( jaxbGenerator.getName() );
		}
		if ( StringHelper.isNotEmpty( jaxbGenerator.getTable() ) ) {
			table( jaxbGenerator.getTable() );
		}
		if ( StringHelper.isNotEmpty( jaxbGenerator.getCatalog() ) ) {
			catalog( jaxbGenerator.getCatalog() );
		}
		if ( StringHelper.isNotEmpty( jaxbGenerator.getSchema() ) ) {
			schema( jaxbGenerator.getSchema() );
		}

		if ( StringHelper.isNotEmpty( jaxbGenerator.getPkColumnName() ) ) {
			pkColumnName( jaxbGenerator.getPkColumnName() );
		}
		if ( StringHelper.isNotEmpty( jaxbGenerator.getPkColumnValue() ) ) {
			pkColumnValue( jaxbGenerator.getPkColumnValue() );
		}

		if ( StringHelper.isNotEmpty( jaxbGenerator.getValueColumnName() ) ) {
			valueColumnName( jaxbGenerator.getValueColumnName() );
		}

		if ( jaxbGenerator.getInitialValue() != null ) {
			initialValue( jaxbGenerator.getInitialValue() );
		}
		if ( jaxbGenerator.getAllocationSize() != null ) {
			allocationSize( jaxbGenerator.getAllocationSize() );
		}

		if ( StringHelper.isNotEmpty( jaxbGenerator.getOptions() ) ) {
			options( jaxbGenerator.getOptions() );
		}

		uniqueConstraints( collectUniqueConstraints( jaxbGenerator.getUniqueConstraints(), xmlDocumentContext ) );
		indexes( collectIndexes( jaxbGenerator.getIndexes(), xmlDocumentContext ) );
	}
}
