/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.annotations.ListIndexBase;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.Join;

import jakarta.persistence.OrderColumn;

import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/**
 * An {@link jakarta.persistence.OrderColumn} annotation
 *
 * @author inger
 */
public class IndexColumn extends AnnotatedColumn {
	private int base;

	public IndexColumn() {
		setLength( 0L );
		setPrecision( 0 );
		setScale( 0 );
	}

	public static IndexColumn fromAnnotations(
			OrderColumn orderColumn,
			org.hibernate.annotations.IndexColumn indexColumn,
			ListIndexBase listIndexBase,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		final IndexColumn column;
		if ( orderColumn != null ) {
			column = buildColumnFromAnnotation( orderColumn, propertyHolder, inferredData, secondaryTables, context );
		}
		else if ( indexColumn != null ) {
			column = buildColumnFromAnnotation( indexColumn, propertyHolder, inferredData, context );
			column.setBase( indexColumn.base() );
		}
		else {
			column = new IndexColumn();
			column.setLogicalColumnName( inferredData.getPropertyName() + "_ORDER" ); //JPA default name
			column.setImplicit( true );
//			column.setContext( context );
//			column.setPropertyHolder( propertyHolder );
			createParent( propertyHolder, secondaryTables, column, context );
			column.bind();
		}

		if ( listIndexBase != null ) {
			column.setBase( listIndexBase.value() );
		}

		return column;
	}

	private static void createParent(
			PropertyHolder propertyHolder,
			Map<String,Join> secondaryTables,
			IndexColumn column,
			MetadataBuildingContext context) {
		final AnnotatedColumns parent = new AnnotatedColumns();
		parent.setPropertyHolder( propertyHolder );
		parent.setJoins( secondaryTables );
		parent.setBuildingContext( context );
		column.setParent( parent );
	}

	public int getBase() {
		return base;
	}

	public void setBase(int base) {
		this.base = base;
	}

	/**
	 * JPA 2 {@link OrderColumn @OrderColumn} processing.
	 *
	 * @param orderColumn The OrderColumn annotation instance
	 * @param propertyHolder Information about the property
	 * @param inferredData Yeah, right.  Uh...
	 * @param secondaryTables Any secondary tables available.
	 *
	 * @return The index column
	 */
	public static IndexColumn buildColumnFromAnnotation(
			OrderColumn orderColumn,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		if ( orderColumn != null ) {
			final String sqlType = nullIfEmpty( orderColumn.columnDefinition() );
			final String name = orderColumn.name().isEmpty()
					? inferredData.getPropertyName() + "_ORDER"
					: orderColumn.name();
			final IndexColumn column = new IndexColumn();
			column.setLogicalColumnName( name );
			column.setColumnDefinition( orderColumn.columnDefinition() );
			column.setSqlType( sqlType );
			column.setNullable( orderColumn.nullable() );
//			column.setJoins( secondaryTables );
			column.setInsertable( orderColumn.insertable() );
			column.setUpdatable( orderColumn.updatable() );
//			column.setContext( context );
//			column.setPropertyHolder( propertyHolder );
			createParent( propertyHolder, secondaryTables, column, context );
			column.bind();
			return column;
		}
		else {
			final IndexColumn column = new IndexColumn();
			column.setImplicit( true );
//			column.setContext( context );
//			column.setPropertyHolder( propertyHolder );
			createParent( propertyHolder, secondaryTables, column, context );
			column.bind();
			return column;
		}
	}

	/**
	 * Legacy {@link IndexColumn @IndexColumn} processing.
	 *
	 * @param indexColumn The IndexColumn annotation instance
	 * @param propertyHolder Information about the property
	 * @param inferredData Yeah, right.  Uh...
	 *
	 * @return The index column
	 */
	public static IndexColumn buildColumnFromAnnotation(
			org.hibernate.annotations.IndexColumn indexColumn,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			MetadataBuildingContext context) {
		if ( indexColumn != null ) {
			final String sqlType = nullIfEmpty( indexColumn.columnDefinition() );
			final String name = indexColumn.name().isEmpty()
					? inferredData.getPropertyName()
					: indexColumn.name();
			//TODO move it to a getter based system and remove the constructor
			final IndexColumn column = new IndexColumn();
			column.setLogicalColumnName( name );
			column.setSqlType( sqlType );
			column.setNullable( indexColumn.nullable() );
			column.setBase( indexColumn.base() );
//			column.setContext( context );
//			column.setPropertyHolder( propertyHolder );
			createParent( propertyHolder, null, column, context );
			column.bind();
			return column;
		}
		else {
			final IndexColumn column = new IndexColumn();
			column.setImplicit( true );
//			column.setContext( context );
//			column.setPropertyHolder( propertyHolder );
			createParent( propertyHolder, null, column, context );
			column.bind();
			return column;
		}
	}
}
