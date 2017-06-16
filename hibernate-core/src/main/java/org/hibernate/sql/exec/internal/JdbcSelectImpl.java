/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.util.List;

import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * @author Steve Ebersole
 */
public class JdbcSelectImpl implements JdbcSelect {
	private final String sql;
	private final List<JdbcParameterBinder> parameterBinders;
	private final List<SqlSelection> sqlSelections;
	private final List<QueryResult> returns;

	public JdbcSelectImpl(
			String sql,
			List<JdbcParameterBinder> parameterBinders,
			List<SqlSelection> sqlSelections,
			List<QueryResult> returns) {
		this.sql = sql;
		this.parameterBinders = parameterBinders;
		this.sqlSelections = sqlSelections;
		this.returns = returns;
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
	public List<SqlSelection> getSqlSelections() {
		return sqlSelections;
	}

	@Override
	public List<QueryResult> getReturns() {
		return returns;
	}
}
