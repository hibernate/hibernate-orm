/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.sql.Template;

import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.STRUCT_ARRAY;
import static org.hibernate.type.SqlTypes.STRUCT_TABLE;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

/**
 * An aggregate column is a column of type {@link org.hibernate.type.SqlTypes#STRUCT},
 * {@link org.hibernate.type.SqlTypes#JSON} or {@link org.hibernate.type.SqlTypes#SQLXML}
 * that aggregates a component into a single column.
 */
public final class AggregateColumn extends Column {

	private final Component component;

	public AggregateColumn(Column column, Component component) {
		setLength( column.getLength() );
		setPrecision( column.getPrecision() );
		setScale( column.getScale() );
		setArrayLength( column.getArrayLength() );
		setValue( column.getValue() );
		setTypeIndex( column.getTypeIndex() );
		setName( column.getQuotedName() );
		setNullable( column.isNullable() );
		setUnique( column.isUnique() );
		setUniqueKeyName( column.getUniqueKeyName() );
		setSqlType( column.getSqlType() );
		setSqlTypeCode( column.getSqlTypeCode() );
		uniqueInteger = column.uniqueInteger; //usually useless
		for ( CheckConstraint constraint : column.getCheckConstraints() ) {
			addCheckConstraint( constraint );
		}
		setComment( column.getComment() );
		setCollation( column.getCollation() );
		setDefaultValue( column.getDefaultValue() );
		setGeneratedAs( column.getGeneratedAs() );
		setAssignmentExpression( column.getAssignmentExpression() );
		setCustomRead( column.getCustomRead() );
		setCustomWrite( column.getCustomWrite() );
		this.component = component;
	}

	public Component getComponent() {
		return component;
	}

	public SelectablePath getSelectablePath() {
		return getSelectablePath( component );
	}

	private static SelectablePath getSelectablePath(Component component) {
		final var aggregateColumn = component.getAggregateColumn();
		final var parent = component.getParentAggregateColumn();
		final String simpleAggregateName = aggregateColumn.getQuotedName();
		return parent == null
				? new SelectablePath( simpleAggregateName )
				: getSelectablePath( parent.getComponent() ).append( simpleAggregateName );
	}

	public String getAggregateReadExpressionTemplate(Dialect dialect) {
		return getAggregateReadExpressionTemplate( dialect, component );
	}

	private static String getAggregateReadExpressionTemplate(Dialect dialect, Component component) {
		final var aggregateColumn = component.getAggregateColumn();
		final var parent = component.getParentAggregateColumn();
		final String simpleAggregateName = aggregateColumn.getQuotedName( dialect );
		// If the aggregate column is an array, drop the parent read expression, because this is a NestedColumnReference
		// and will require special rendering
		return parent == null || isArray( aggregateColumn )
				? getRootAggregateSelectableExpression( aggregateColumn, simpleAggregateName )
				: dialect.getAggregateSupport()
						.aggregateComponentCustomReadExpression(
								"",
								"",
								getAggregateReadExpressionTemplate( dialect, parent.getComponent() ),
								simpleAggregateName,
								parent,
								aggregateColumn
						);
	}

	private static String getRootAggregateSelectableExpression(AggregateColumn aggregateColumn, String simpleAggregateName) {
		return isArray( aggregateColumn ) ? Template.TEMPLATE : Template.TEMPLATE + "." + simpleAggregateName;
	}

	private static boolean isArray(AggregateColumn aggregateColumn) {
		return switch ( aggregateColumn.getTypeCode() ) {
			case JSON_ARRAY, XML_ARRAY, STRUCT_ARRAY, STRUCT_TABLE -> true;
			default -> false;
		};
	}

	public String getAggregateAssignmentExpressionTemplate(Dialect dialect) {
		return getAggregateAssignmentExpressionTemplate( dialect, component );
	}

	private static String getAggregateAssignmentExpressionTemplate(Dialect dialect, Component component) {
		final var aggregateColumn = component.getAggregateColumn();
		final var parent = component.getParentAggregateColumn();
		final String simpleAggregateName = aggregateColumn.getQuotedName( dialect );
		return parent == null
				? Template.TEMPLATE + "." + simpleAggregateName
				: dialect.getAggregateSupport()
						.aggregateComponentAssignmentExpression(
								getAggregateAssignmentExpressionTemplate( dialect, parent.getComponent() ),
								simpleAggregateName,
								parent,
								aggregateColumn
						);
	}

	/**
	 * Shallow copy, the value is not copied
	 */
	@Override
	public AggregateColumn clone() {
		return new AggregateColumn( this, component );
	}
}
