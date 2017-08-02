/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.List;

import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.results.spi.ResultSetMapping;

/**
 * @author Steve Ebersole
 */
public class JdbcSelectImpl implements JdbcSelect {
	private final String sql;
	private final List<JdbcParameterBinder> parameterBinders;
	private final ResultSetMapping resultSetMapping;

	public JdbcSelectImpl(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			ResultSetMapping resultSetMapping) {
		this.sql = sql;
		this.parameterBinders = parameterBinders;
		this.resultSetMapping = resultSetMapping;
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
	public ResultSetMapping getResultSetMapping() {
		return resultSetMapping;
	}
}
