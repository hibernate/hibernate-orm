/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
public class AggregateColumn extends Column {

	private final Component component;

	public AggregateColumn(Column column, Component component) {
		setColumnDefinition( column.getColumnDefinition() );
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
		final AggregateColumn aggregateColumn = component.getAggregateColumn();
		final AggregateColumn parentAggregateColumn = component.getParentAggregateColumn();
		final String simpleAggregateName = aggregateColumn.getQuotedName();
		if ( parentAggregateColumn == null ) {
			return new SelectablePath( simpleAggregateName );
		}
		else {
			return getSelectablePath( parentAggregateColumn.getComponent() ).append( simpleAggregateName );
		}
	}

	public String getAggregateReadExpressionTemplate(Dialect dialect) {
		return getAggregateReadExpressionTemplate( dialect, component );
	}

	private static String getAggregateReadExpressionTemplate(Dialect dialect, Component component) {
		final AggregateColumn aggregateColumn = component.getAggregateColumn();
		final AggregateColumn parentAggregateColumn = component.getParentAggregateColumn();
		final String simpleAggregateName = aggregateColumn.getQuotedName( dialect );
		final String aggregateSelectableExpression;
		// If the aggregate column is an array, drop the parent read expression, because this is a NestedColumnReference
		// and will require special rendering
		if ( parentAggregateColumn == null || isArray( aggregateColumn ) ) {
			aggregateSelectableExpression = getRootAggregateSelectableExpression( aggregateColumn, simpleAggregateName );
		}
		else {
			aggregateSelectableExpression = dialect.getAggregateSupport().aggregateComponentCustomReadExpression(
					"",
					"",
					getAggregateReadExpressionTemplate(
							dialect,
							parentAggregateColumn.getComponent()
					),
					simpleAggregateName,
					parentAggregateColumn,
					aggregateColumn
			);
		}
		return aggregateSelectableExpression;
	}

	private static String getRootAggregateSelectableExpression(AggregateColumn aggregateColumn, String simpleAggregateName) {
		if ( isArray( aggregateColumn ) ) {
			return Template.TEMPLATE;
		}
		else {
			return Template.TEMPLATE + "." + simpleAggregateName;
		}
	}

	private static boolean isArray(AggregateColumn aggregateColumn) {
		switch ( aggregateColumn.getTypeCode() ) {
			case JSON_ARRAY:
			case XML_ARRAY:
			case STRUCT_ARRAY:
			case STRUCT_TABLE:
				return true;
			default:
				return false;
		}
	}

	public String getAggregateAssignmentExpressionTemplate(Dialect dialect) {
		return getAggregateAssignmentExpressionTemplate( dialect, component );
	}

	private static String getAggregateAssignmentExpressionTemplate(Dialect dialect, Component component) {
		final AggregateColumn aggregateColumn = component.getAggregateColumn();
		final AggregateColumn parentAggregateColumn = component.getParentAggregateColumn();
		final String simpleAggregateName = aggregateColumn.getQuotedName( dialect );
		final String aggregateSelectableExpression;
		if ( parentAggregateColumn == null ) {
			aggregateSelectableExpression = Template.TEMPLATE + "." + simpleAggregateName;
		}
		else {
			aggregateSelectableExpression = dialect.getAggregateSupport().aggregateComponentAssignmentExpression(
					getAggregateAssignmentExpressionTemplate(
							dialect,
							parentAggregateColumn.getComponent()
					),
					simpleAggregateName,
					parentAggregateColumn,
					aggregateColumn
			);
		}
		return aggregateSelectableExpression;
	}

	/**
	 * Shallow copy, the value is not copied
	 */
	@Override
	public AggregateColumn clone() {
		return new AggregateColumn( this, component );
	}
}
