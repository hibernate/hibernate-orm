/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueBindingsImplementor;
import org.hibernate.sql.model.TableMapping;

/**
 * @author Steve Ebersole
 */
public class FixUpJdbcValueBindings implements JdbcValueBindingsImplementor {
	@Override
	public Object getBoundValue(String tableName, String columnName, ParameterUsage usage) {
		return null;
	}

	@Override
	public void replaceValue(String tableName, String columnName, ParameterUsage usage, Object newValue) {

	}

	@Override
	public BindingGroup getBindingGroup(String tableName) {
		return null;
	}

	@Override
	public void bindValue(Object value, String tableName, String columnName, ParameterUsage usage) {

	}

	@Override
	public void beforeStatement(PreparedStatementDetails statementDetails) {

	}

	@Override
	public void afterStatement(TableMapping mutatingTable) {

	}
}
