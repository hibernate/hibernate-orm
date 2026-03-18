/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.Helper;
import org.hibernate.action.queue.op.JdbcValueDescriptorImpl;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/// Immutable descriptor for a column involved in a mutation.
///
/// Extends existing ColumnDetails with additional mutation context.
///
/// @param normalizedName Pre-normalized column name - see [org.hibernate.action.queue.Helper#normalizeColumnName(String)]
/// @param physicalName Original physical name.
/// @param jdbcMapping Details about the JDBC type of the column.
///
/// @author Steve Ebersole
public record ColumnDescriptor(
		String normalizedName,
		String physicalName,
		String normalizedTableName,
		JdbcMapping jdbcMapping,
		String writeFragment,
		boolean isFormula,
		boolean nullable,
		boolean insertable,
		boolean updatable,
		boolean isPartitioned) implements SelectableMapping {

	public static ColumnDescriptor from(SelectableMapping selectable) {
		return new ColumnDescriptor(
				Helper.normalizeColumnName(selectable.getSelectableName()),
				selectable.getSelectableName(),
				Helper.normalizeTableName( selectable.getContainingTableExpression() ),
				selectable.getJdbcMapping(),
				selectable.getWriteExpression(),
				selectable.isFormula(),
				selectable.isNullable(),
				selectable.isInsertable(),
				selectable.isUpdateable(),
				selectable.isPartitioned()
		);
	}

	public JdbcValueDescriptor createValueDescriptor(ParameterUsage parameterUsage, int parameterIndex) {
		return new JdbcValueDescriptorImpl( normalizedName, jdbcMapping, parameterUsage, parameterIndex );
	}

	@Override
	public String getContainingTableExpression() {
		return normalizedTableName;
	}

	@Override
	public String getSelectionExpression() {
		return physicalName;
	}

	@Override
	public @Nullable String getCustomReadExpression() {
		return null;
	}

	@Override
	public String getCustomWriteExpression() {
		return writeFragment;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public boolean isInsertable() {
		return insertable;
	}

	@Override
	public boolean isUpdateable() {
		return updatable;
	}

	@Override
	public @Nullable String getColumnDefinition() {
		return null;
	}

	@Override
	public @Nullable Long getLength() {
		return null;
	}

	@Override
	public @Nullable Integer getArrayLength() {
		return null;
	}

	@Override
	public @Nullable Integer getPrecision() {
		return null;
	}

	@Override
	public @Nullable Integer getScale() {
		return null;
	}

	@Override
	public @Nullable Integer getTemporalPrecision() {
		return null;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}
}
