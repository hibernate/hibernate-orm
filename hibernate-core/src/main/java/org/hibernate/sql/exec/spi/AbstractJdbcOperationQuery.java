/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
				final JdbcParameterBinding binding = jdbcParameterBindings.getBinding( entry.getKey() );
				final JdbcParameterBinding appliedBinding = entry.getValue();
				//noinspection unchecked
				if ( binding == null || !appliedBinding.getBindType()
						.getJavaTypeDescriptor()
						.areEqual( binding.getBindValue(), appliedBinding.getBindValue() ) ) {
					return false;
				}
			}
		}
		return true;
	}
}
