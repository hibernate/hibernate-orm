/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * Executable JDBC command
 *
 * @author Christian Beikov
 */
public class JdbcOperationQueryMutationNative implements JdbcOperationQueryMutation {
	private final String sql;
	private final List<JdbcParameterBinder> parameterBinders;
	private final Set<String> affectedTableNames;

	public JdbcOperationQueryMutationNative(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames) {
		this.sql = sql;
		this.parameterBinders = parameterBinders;
		this.affectedTableNames = affectedTableNames;
	}

	@Override
	public String getSqlString() {
		return sql;
	}

	@Override
	public List<JdbcParameterBinder> getParameterBinders() {
		return parameterBinders;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	@Override
	public boolean dependsOnParameterBindings() {
		return false;
	}

	@Override
	public Map<JdbcParameter, JdbcParameterBinding> getAppliedParameters() {
		return Collections.emptyMap();
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		return true;
	}
}
