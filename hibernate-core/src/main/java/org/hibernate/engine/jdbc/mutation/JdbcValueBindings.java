/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.mutation;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
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
}
