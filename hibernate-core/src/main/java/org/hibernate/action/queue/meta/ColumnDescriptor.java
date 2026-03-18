/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.op.JdbcValueDescriptorImpl;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

import java.io.Serializable;

/// Immutable descriptor for a column involved in a mutation.
///
/// Extends existing ColumnDetails with additional mutation context.
///
/// @param name Column name from the mapping model.
/// @param tableName Table name containing this column.
/// @param jdbcMapping Details about the JDBC type of the column.
///
/// @author Steve Ebersole
public record ColumnDescriptor(
		String name,
		String tableName,
		JdbcMapping jdbcMapping,
		String writeFragment,
		boolean isFormula,
		boolean nullable,
		boolean insertable,
		boolean updatable,
		boolean isPartitioned) implements SelectableMapping, Serializable {

	public static ColumnDescriptor from(SelectableMapping selectable) {
		return new ColumnDescriptor(
				selectable.getSelectableName(),
				selectable.getContainingTableExpression(),
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
		return new JdbcValueDescriptorImpl( name, jdbcMapping, parameterUsage, parameterIndex );
	}

	@Override
	public String getContainingTableExpression() {
		return tableName;
	}

	@Override
	public String getSelectionExpression() {
		return name;
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
