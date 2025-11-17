/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
			ListIndexBase listIndexBase,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		final IndexColumn column;
		if ( orderColumn != null ) {
			column = buildColumnFromOrderColumn( orderColumn, propertyHolder, inferredData, secondaryTables, context );
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
		final var parent = new AnnotatedColumns();
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
	public static IndexColumn buildColumnFromOrderColumn(
			OrderColumn orderColumn,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		if ( orderColumn != null ) {
			final String sqlType = nullIfEmpty( orderColumn.columnDefinition() );
			final String explicitName = orderColumn.name();
			final String name = explicitName.isBlank()
					? inferredData.getPropertyName() + "_ORDER"
					: explicitName;
			final IndexColumn column = new IndexColumn();
			column.setLogicalColumnName( name );
			column.setSqlType( sqlType );
			column.setNullable( orderColumn.nullable() );
//			column.setJoins( secondaryTables );
			column.setInsertable( orderColumn.insertable() );
			column.setUpdatable( orderColumn.updatable() );
			column.setOptions( orderColumn.options() );
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
}
