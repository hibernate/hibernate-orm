/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.JaxbOrderColumnImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.ColumnDetails;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.OrderColumn;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class OrderColumnJpaAnnotation implements OrderColumn,
		ColumnDetails,
		ColumnDetails.Definable,
		ColumnDetails.Nullable,
		ColumnDetails.Mutable {
	private String name;
	private boolean nullable;
	private boolean insertable;
	private boolean updatable;
	private String columnDefinition;
	private String options;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public OrderColumnJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.nullable = true;
		this.insertable = true;
		this.updatable = true;
		this.columnDefinition = "";
		this.options = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public OrderColumnJpaAnnotation(OrderColumn annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.nullable = annotation.nullable();
		this.insertable = annotation.insertable();
		this.updatable = annotation.updatable();
		this.columnDefinition = annotation.columnDefinition();
		this.options = annotation.options();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public OrderColumnJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.ORDER_COLUMN, "name", modelContext );
		this.nullable = extractJandexValue( annotation, JpaAnnotations.ORDER_COLUMN, "nullable", modelContext );
		this.insertable = extractJandexValue( annotation, JpaAnnotations.ORDER_COLUMN, "insertable", modelContext );
		this.updatable = extractJandexValue( annotation, JpaAnnotations.ORDER_COLUMN, "updatable", modelContext );
		this.columnDefinition = extractJandexValue(
				annotation,
				JpaAnnotations.ORDER_COLUMN,
				"columnDefinition",
				modelContext
		);
		this.options = extractJandexValue( annotation, JpaAnnotations.ORDER_COLUMN, "options", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return OrderColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
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


	public void apply(JaxbOrderColumnImpl jaxbColumn, XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			name( jaxbColumn.getName() );
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
	}
}
