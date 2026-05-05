/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.TableMapping;

/**
 * The JDBC values for a mutation
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JdbcValueBindings {
	/**
	 * Get the bindings for the specific table, or {@code null}
	 */
	BindingGroup getBindingGroup(String tableName);

	/**
	 * Binds a value for a specific column+usage
	 */
	void bindValue(Object value, String tableName, String columnName, ParameterUsage usage);

	/**
	 * Binds a value for a specific column+usage
	 */
	default void bindValue(Object value, SelectableMapping selectableMapping, ParameterUsage usage) {
		bindValue( value, selectableMapping.getContainingTableExpression(), selectableMapping.getSelectionExpression(), usage );
	}

	/**
	 * Called before the execution of the operation for the specified table
	 */
	void beforeStatement(PreparedStatementDetails statementDetails);

	/**
	 * Called after the execution of the operation for the specified table
	 */
	void afterStatement(TableMapping mutatingTable);

	/**
	 * Form of {@linkplain #bindValue(Object, SelectableMapping, ParameterUsage)} which is intended for use
	 * as a {@linkplain ModelPart.JdbcValueConsumer} with {@linkplain ParameterUsage#SET} semantics.
	 */
	default void bindAssignment(int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
		bindValue( value, jdbcValueMapping, ParameterUsage.SET );
	}

	/**
	 * Form of {@linkplain #bindValue(Object, SelectableMapping, ParameterUsage)} which is intended for use
	 * as a {@linkplain ModelPart.JdbcValueConsumer} with {@linkplain ParameterUsage#RESTRICT} semantics.
	 */
	default void bindRestriction(int valueIndex, Object value, SelectableMapping jdbcValueMapping) {
		bindValue( value, jdbcValueMapping, ParameterUsage.RESTRICT );
	}
}
