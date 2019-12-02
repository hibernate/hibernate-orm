/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import java.util.List;
import java.util.Map;

import org.hibernate.query.sqm.sql.internal.StandardSqmSelectTranslator;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * Details of the result of interpreting an SQM SELECT AST into a SQL SELECT AST
 *
 * @see StandardSqmSelectTranslator#translate(org.hibernate.query.sqm.tree.select.SqmSelectStatement)
 *
 * @author Steve Ebersole
 */
public class SqmSelectTranslation implements SqmTranslation {
	private final SelectStatement sqlAst;
	private final Map<SqmParameter,List<JdbcParameter>> jdbcParamsBySqmParam;

	public SqmSelectTranslation(
			SelectStatement sqlAst,
			Map<SqmParameter, List<JdbcParameter>> jdbcParamsBySqmParam) {
		this.sqlAst = sqlAst;
		this.jdbcParamsBySqmParam = jdbcParamsBySqmParam;
	}

	@Override
	public SelectStatement getSqlAst() {
		return sqlAst;
	}

	@Override
	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamsBySqmParam;
	}
}
