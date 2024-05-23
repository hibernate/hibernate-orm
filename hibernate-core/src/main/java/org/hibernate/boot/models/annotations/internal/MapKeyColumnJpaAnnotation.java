/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyColumnImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.spi.ColumnDetails;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.Column;
import jakarta.persistence.MapKeyColumn;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyColumnJpaAnnotation implements MapKeyColumn,
		ColumnDetails.Nullable,
		ColumnDetails.Mutable,
		ColumnDetails.Sizable,
		ColumnDetails.Uniqueable,
		ColumnDetails.Definable,
		ColumnDetails.AlternateTableCapable {
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

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyColumnJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.unique = false;
		this.nullable = false;
		this.insertable = true;
		this.updatable = true;
		this.columnDefinition = "";
		this.options = "";
		this.table = "";
		this.length = 255;
		this.precision = 0;
		this.scale = 0;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyColumnJpaAnnotation(MapKeyColumn annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "name", modelContext );
		this.unique = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "unique", modelContext );
		this.nullable = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "nullable", modelContext );
		this.insertable = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "insertable", modelContext );
		this.updatable = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "updatable", modelContext );
		this.columnDefinition = extractJdkValue(
				annotation,
				JpaAnnotations.MAP_KEY_COLUMN,
				"columnDefinition",
				modelContext
		);
		this.options = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "options", modelContext );
		this.table = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "table", modelContext );
		this.length = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "length", modelContext );
		this.precision = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "precision", modelContext );
		this.scale = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "scale", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyColumnJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "name", modelContext );
		this.unique = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "unique", modelContext );
		this.nullable = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "nullable", modelContext );
		this.insertable = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "insertable", modelContext );
		this.updatable = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "updatable", modelContext );
		this.columnDefinition = extractJandexValue(
				annotation,
				JpaAnnotations.MAP_KEY_COLUMN,
				"columnDefinition",
				modelContext
		);
		this.options = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "options", modelContext );
		this.table = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "table", modelContext );
		this.length = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "length", modelContext );
		this.precision = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "precision", modelContext );
		this.scale = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_COLUMN, "scale", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKeyColumn.class;
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


	public void apply(JaxbMapKeyColumnImpl jaxbColumn, XmlDocumentContext xmlDocumentContext) {
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

		if ( jaxbColumn.getLength() != null ) {
			length( jaxbColumn.getLength() );
		}

		if ( jaxbColumn.getPrecision() != null ) {
			precision( jaxbColumn.getPrecision() );
		}

		if ( jaxbColumn.getScale() != null ) {
			scale( jaxbColumn.getScale() );
		}
	}

	public static Column toColumnAnnotation(
			MapKeyColumn mapKeyColumn,
			SourceModelBuildingContext sourceModelBuildingContext) {
		final ColumnJpaAnnotation column = new ColumnJpaAnnotation( sourceModelBuildingContext );
		column.name( mapKeyColumn.name() );
		column.table( mapKeyColumn.table() );
		column.nullable( mapKeyColumn.nullable() );
		column.unique( mapKeyColumn.unique() );
		column.insertable( mapKeyColumn.insertable() );
		column.updatable( mapKeyColumn.updatable() );
		column.columnDefinition( mapKeyColumn.columnDefinition() );
		column.options( mapKeyColumn.options() );
		column.length( mapKeyColumn.length() );
		column.precision( mapKeyColumn.precision() );
		column.scale( mapKeyColumn.scale() );
		return column;
	}
}
