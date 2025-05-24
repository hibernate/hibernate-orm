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
 * @author Steve Ebersole
 */
public class AbstractJdbcOperationQuery implements JdbcOperationQuery {
	protected final String sql;
	protected final List<JdbcParameterBinder> parameterBinders;
	protected final Set<String> affectedTableNames;
	protected final Map<JdbcParameter, JdbcParameterBinding> appliedParameters;

	public AbstractJdbcOperationQuery(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames) {
		this(
				sql,
				parameterBinders,
				affectedTableNames,
				Collections.emptyMap()
		);
	}

	public AbstractJdbcOperationQuery(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames,
			Map<JdbcParameter, JdbcParameterBinding> appliedParameters) {
		this.sql = sql;
		this.parameterBinders = parameterBinders;
		this.affectedTableNames = affectedTableNames;
		this.appliedParameters = appliedParameters;
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
		return !appliedParameters.isEmpty();
	}

	@Override
	public Map<JdbcParameter, JdbcParameterBinding> getAppliedParameters() {
		return appliedParameters;
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		if ( !appliedParameters.isEmpty() ) {
			if ( jdbcParameterBindings == null ) {
				return false;
			}
			for ( Map.Entry<JdbcParameter, JdbcParameterBinding> entry : appliedParameters.entrySet() ) {
				final JdbcParameter parameter = entry.getKey();
				final JdbcParameterBinding binding = jdbcParameterBindings.getBinding( parameter );
				final JdbcParameterBinding appliedBinding = entry.getValue();
				if ( !isCompatible( parameter, appliedBinding, binding, queryOptions ) ) {
					return false;
				}
			}
		}
		return true;
	}

	protected boolean isCompatible(
			JdbcParameter parameter,
			JdbcParameterBinding appliedBinding,
			JdbcParameterBinding binding,
			QueryOptions queryOptions) {
		// This is a special case where the rendered SQL depends on the presence of the parameter,
		// but not specifically on the value. Some example of such situation are when generating
		// SQL that depends on the type of the parameter (e.g., cast). See also HHH-19331
		// and QueryPlanCachingTest, as well as subclass overrides of this method.
		if ( appliedBinding == null ) {
			// We could optionally optimize this by identifying only the type and checking it in the same
			// way we check the value below.
			return false;
		}
		//noinspection unchecked
		if ( binding == null || !appliedBinding.getBindType()
				.getJavaTypeDescriptor()
				.areEqual( binding.getBindValue(), appliedBinding.getBindValue() ) ) {
			return false;
		}
		return true;
	}
}
