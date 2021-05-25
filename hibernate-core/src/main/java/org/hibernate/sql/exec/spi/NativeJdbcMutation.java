/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.FilterJdbcParameter;
import org.hibernate.query.spi.QueryOptions;

/**
 * Executable JDBC command
 *
 * @author Christian Beikov
 */
public class NativeJdbcMutation implements JdbcMutation {
	private final String sql;
	private final List<JdbcParameterBinder> parameterBinders;
	private final Set<String> affectedTableNames;

	public NativeJdbcMutation(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			Set<String> affectedTableNames) {
		this.sql = sql;
		this.parameterBinders = parameterBinders;
		this.affectedTableNames = affectedTableNames;
	}

	@Override
	public String getSql() {
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
	public Set<FilterJdbcParameter> getFilterJdbcParameters() {
		return Collections.EMPTY_SET;
	}

	@Override
	public boolean dependsOnParameterBindings() {
		return false;
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		return true;
	}
}
