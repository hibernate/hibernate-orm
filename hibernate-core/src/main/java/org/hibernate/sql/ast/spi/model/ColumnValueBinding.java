package org.hibernate.sql.ast.spi.model;

import java.util.Locale;
import java.util.Objects;

import jakarta.annotation.Nullable;

import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;

/**
 * Represents the binding of a value to a column.
 * Uniformly models both assignments and restrictions in
 * relation to inserts, updates and upserts.
 *
 * @author Steve Ebersole
 */
public class ColumnValueBinding {
	private final ColumnReference columnReference;
	private final ColumnWriteFragment valueExpression;
	private final boolean attributeInsertable;
	private final boolean attributeUpdatable;

	public ColumnValueBinding(ColumnReference columnReference, ColumnWriteFragment valueExpression) {
		this( columnReference, valueExpression, determineAttributeInsertable( valueExpression ) );
	}

	public ColumnValueBinding(ColumnReference columnReference, ColumnWriteFragment valueExpression, boolean insertable) {
		this.columnReference = columnReference;
		this.valueExpression = valueExpression;
		this.attributeInsertable = insertable;
		this.attributeUpdatable = determineAttributeUpdatable( valueExpression );
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}

	public ColumnWriteFragment getValueExpression() {
		return valueExpression;
	}

	public boolean isAttributeInsertable() {
		return attributeInsertable;
	}

	public boolean isAttributeUpdatable() {
		return attributeUpdatable;
	}

	/// The assignment made when an existing row is updated, which may differ
	/// from the value stored by an insert.
	///
	/// @param targetAlias the alias of the updated table, or `null` if its
	///                    columns are not qualified
	public ColumnValueBinding forUpdate(@Nullable String targetAlias) {
		return this;
	}

	/// Whether the value stored by an update depends on the existing row, so
	/// that it differs from the value stored by an insert, and is known only
	/// to the database.
	public boolean isUpdateConditional() {
		return false;
	}

	/// Whether the value is an SQL expression which only an update assigns.
	/// Such an expression may refer to the existing row.
	public boolean isUpdateExpression() {
		return !attributeInsertable && valueExpression != null && valueExpression.getParameters().isEmpty();
	}

	private static boolean determineAttributeInsertable(ColumnWriteFragment valueExpression) {
		return valueExpression == null
			|| !(valueExpression.getSqlTypedMapping() instanceof SelectableMapping selectableMapping)
			|| selectableMapping.isInsertable();
	}

	private static boolean determineAttributeUpdatable(ColumnWriteFragment valueExpression) {
		return valueExpression == null
			|| !(valueExpression.getSqlTypedMapping() instanceof SelectableMapping selectableMapping)
			|| selectableMapping.isUpdateable();
	}

	public boolean matches(SelectableMapping selectableMapping) {
		return columnReference.isColumnExpressionFormula() == selectableMapping.isFormula()
			&& Objects.equals( columnReference.getColumnExpression(), selectableMapping.getSelectionExpression() );
	}

	@Override
	public String toString() {
		return String.format( Locale.ROOT, "ColumnValueBinding(%s=%s)",
				columnReference.getColumnExpression(),
				valueExpressionString( valueExpression )
		);
	}

	private String valueExpressionString(ColumnWriteFragment valueExpression) {
		return switch ( valueExpression.getParameters().size() ) {
			case 0 -> valueExpression.getFragment();
			case 1 -> "?";
			default -> "(?,...)";
		};
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ColumnValueBinding that = (ColumnValueBinding) o;
		return Objects.equals( columnReference, that.columnReference );
	}

	@Override
	public int hashCode() {
		return Objects.hash( columnReference );
	}
}
