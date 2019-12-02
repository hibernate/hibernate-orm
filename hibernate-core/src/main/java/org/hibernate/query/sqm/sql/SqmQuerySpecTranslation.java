/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import java.util.List;
import java.util.Map;

import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class SqmQuerySpecTranslation {
	private final QuerySpec sqlAst;
	private final Map<SqmParameter, List<JdbcParameter>> jdbcParamsBySqmParam;

	public SqmQuerySpecTranslation(
			QuerySpec sqlAst,
			Map<SqmParameter, List<JdbcParameter>> jdbcParamsBySqmParam) {
		this.sqlAst = sqlAst;
		this.jdbcParamsBySqmParam = jdbcParamsBySqmParam;
	}

	public QuerySpec getSqlAst() {
		return sqlAst;
	}

	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamsBySqmParam;
	}
}
