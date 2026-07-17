/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.type.MappingContext;
import org.hibernate.sql.Template;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.STRUCT_ARRAY;
import static org.hibernate.type.SqlTypes.STRUCT_TABLE;
import static org.hibernate.type.SqlTypes.TABLE;
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

	public JdbcType getJdbcType(MappingContext mappingContext) {
		return getType( mappingContext ).getJdbcType();
	}

	public SelectablePath getSelectablePath() {
		return getSelectablePath( component );
	}

	public boolean isAggregateArray() {
		return switch ( getTypeCode() ) {
			case ARRAY, TABLE, JSON_ARRAY, XML_ARRAY, STRUCT_ARRAY, STRUCT_TABLE -> true;
			default -> false;
		};
	}

	private static SelectablePath getSelectablePath(Component component) {
		final var aggregateColumn = component.getAggregateColumn();
		final var parent = component.getParentAggregateColumn();
		final String simpleAggregateName = aggregateColumn.getQuotedName();
		return parent == null
				? new SelectablePath( simpleAggregateName )
				: getSelectablePath( parent.getComponent() ).append( simpleAggregateName );
	}

	public String getAggregateReadExpressionTemplate(
			Dialect dialect,
			MappingContext mappingContext,
			TypeConfiguration typeConfiguration) {
		return getAggregateReadExpressionTemplate( dialect, mappingContext, typeConfiguration, component );
	}

	private static String getAggregateReadExpressionTemplate(
			Dialect dialect,
			MappingContext mappingContext,
			TypeConfiguration typeConfiguration,
			Component component) {
		final var aggregateColumn = component.getAggregateColumn();
		final var parent = component.getParentAggregateColumn();
		final String simpleAggregateName = aggregateColumn.getQuotedName( dialect );
		// If the aggregate column is an array, drop the parent read expression, because this is a NestedColumnReference
		// and will require special rendering
		return parent == null || aggregateColumn.isAggregateArray()
				? getRootAggregateSelectableExpression( aggregateColumn, simpleAggregateName )
				: dialect.getAggregateSupport()
						.aggregateComponentCustomReadExpression(
								"",
								"",
								getAggregateReadExpressionTemplate(
										dialect,
										mappingContext,
										typeConfiguration,
										parent.getComponent()
								),
								simpleAggregateName,
								parent,
								aggregateColumn,
								mappingContext,
								typeConfiguration
						);
	}

	private static String getRootAggregateSelectableExpression(AggregateColumn aggregateColumn, String simpleAggregateName) {
		return aggregateColumn.isAggregateArray() ? Template.TEMPLATE : Template.TEMPLATE + "." + simpleAggregateName;
	}

	public String getAggregateAssignmentExpressionTemplate(Dialect dialect, MappingContext mappingContext) {
		return getAggregateAssignmentExpressionTemplate( dialect, mappingContext, component );
	}

	private static String getAggregateAssignmentExpressionTemplate(
			Dialect dialect,
			MappingContext mappingContext,
			Component component) {
		final var aggregateColumn = component.getAggregateColumn();
		final var parent = component.getParentAggregateColumn();
		final String simpleAggregateName = aggregateColumn.getQuotedName( dialect );
		return parent == null
				? Template.TEMPLATE + "." + simpleAggregateName
				: dialect.getAggregateSupport()
						.aggregateComponentAssignmentExpression(
								getAggregateAssignmentExpressionTemplate( dialect, mappingContext, parent.getComponent() ),
								simpleAggregateName,
								parent,
								aggregateColumn,
								mappingContext
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
