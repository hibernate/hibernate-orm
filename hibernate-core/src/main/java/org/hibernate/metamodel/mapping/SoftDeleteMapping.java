/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

/**
 *
 * Metadata about the indicator column for entities and collections enabled
 * for soft delete
 *
 * @see org.hibernate.annotations.SoftDelete
 *
 * @author Steve Ebersole
 */
public interface SoftDeleteMapping extends SelectableMapping, VirtualModelPart, SqlExpressible {
	/**
	 * The name of the soft-delete indicator column.
	 */
	String getColumnName();

	/**
	 * The name of the table which holds the {@linkplain #getColumnName() indicator column}
	 */
	String getTableName();

	/**
	 * The SQL literal value which indicates a deleted row
	 */
	Object getDeletedLiteralValue();

	/**
	 * The String representation of the SQL literal value which indicates a deleted row
	 */
	String getDeletedLiteralText();

	/**
	 * The SQL literal value which indicates a non-deleted row
	 *
	 * @apiNote The inverse of {@linkplain #getDeletedLiteralValue()}
	 */
	Object getNonDeletedLiteralValue();

	/**
	 * The String representation of the SQL literal value which indicates a non-deleted row
	 */
	String getNonDeletedLiteralText();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SelectableMapping

	@Override
	default String getSelectionExpression() {
		return getColumnName();
	}

	@Override
	default String getSelectableName() {
		return getColumnName();
	}

	@Override
	default String getWriteExpression() {
		return getNonDeletedLiteralText();
	}

	@Override
	default String getContainingTableExpression() {
		return getTableName();
	}

	@Override
	default String getCustomReadExpression() {
		return null;
	}

	@Override
	default String getCustomWriteExpression() {
		return null;
	}

	@Override
	default boolean isFormula() {
		return false;
	}

	@Override
	default boolean isNullable() {
		return false;
	}

	@Override
	default boolean isInsertable() {
		return true;
	}

	@Override
	default boolean isUpdateable() {
		return true;
	}

	@Override
	default boolean isPartitioned() {
		return false;
	}

	@Override
	default String getColumnDefinition() {
		return null;
	}

	@Override
	default Long getLength() {
		return null;
	}

	@Override
	default Integer getPrecision() {
		return null;
	}

	@Override
	default Integer getScale() {
		return null;
	}

	@Override
	default Integer getTemporalPrecision() {
		return null;
	}
}
