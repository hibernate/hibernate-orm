/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.IndexCollector;
import org.hibernate.boot.models.annotations.spi.UniqueConstraintCollector;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.TableGenerator;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
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
	public TableGeneratorJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
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
	public TableGeneratorJpaAnnotation(TableGenerator annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, JpaAnnotations.TABLE_GENERATOR, "name", modelContext );
		this.table = extractJdkValue( annotation, JpaAnnotations.TABLE_GENERATOR, "table", modelContext );
		this.catalog = extractJdkValue( annotation, JpaAnnotations.TABLE_GENERATOR, "catalog", modelContext );
		this.schema = extractJdkValue( annotation, JpaAnnotations.TABLE_GENERATOR, "schema", modelContext );
		this.pkColumnName = extractJdkValue( annotation, JpaAnnotations.TABLE_GENERATOR, "pkColumnName", modelContext );
		this.valueColumnName = extractJdkValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"valueColumnName",
				modelContext
		);
		this.pkColumnValue = extractJdkValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"pkColumnValue",
				modelContext
		);
		this.initialValue = extractJdkValue( annotation, JpaAnnotations.TABLE_GENERATOR, "initialValue", modelContext );
		this.allocationSize = extractJdkValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"allocationSize",
				modelContext
		);
		this.uniqueConstraints = extractJdkValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"uniqueConstraints",
				modelContext
		);
		this.indexes = extractJdkValue( annotation, JpaAnnotations.TABLE_GENERATOR, "indexes", modelContext );
		this.options = extractJdkValue( annotation, JpaAnnotations.TABLE_GENERATOR, "options", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public TableGeneratorJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.TABLE_GENERATOR, "name", modelContext );
		this.table = extractJandexValue( annotation, JpaAnnotations.TABLE_GENERATOR, "table", modelContext );
		this.catalog = extractJandexValue( annotation, JpaAnnotations.TABLE_GENERATOR, "catalog", modelContext );
		this.schema = extractJandexValue( annotation, JpaAnnotations.TABLE_GENERATOR, "schema", modelContext );
		this.pkColumnName = extractJandexValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"pkColumnName",
				modelContext
		);
		this.valueColumnName = extractJandexValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"valueColumnName",
				modelContext
		);
		this.pkColumnValue = extractJandexValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"pkColumnValue",
				modelContext
		);
		this.initialValue = extractJandexValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"initialValue",
				modelContext
		);
		this.allocationSize = extractJandexValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"allocationSize",
				modelContext
		);
		this.uniqueConstraints = extractJandexValue(
				annotation,
				JpaAnnotations.TABLE_GENERATOR,
				"uniqueConstraints",
				modelContext
		);
		this.indexes = extractJandexValue( annotation, JpaAnnotations.TABLE_GENERATOR, "indexes", modelContext );
		this.options = extractJandexValue( annotation, JpaAnnotations.TABLE_GENERATOR, "options", modelContext );
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
