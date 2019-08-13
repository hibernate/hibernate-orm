/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * Details of the result of interpreting an SQM SELECT AST into a SQL SELECT AST
 *
 * @see SqmSelectToSqlAstConverter#interpret(org.hibernate.query.sqm.tree.select.SqmSelectStatement)
 *
 * @author Steve Ebersole
 */
public class SqmSelectInterpretation {
	private final SelectStatement sqlAst;
	private final Set<String> affectedTableNames;
	private final Map<SqmParameter,List<JdbcParameter>> jdbcParamsBySqmParam;

	public SqmSelectInterpretation(
			SelectStatement sqlAst,
			Set<String> affectedTableNames,
			Map<SqmParameter, List<JdbcParameter>> jdbcParamsBySqmParam) {
		this.sqlAst = sqlAst;
		this.affectedTableNames = affectedTableNames;
		this.jdbcParamsBySqmParam = jdbcParamsBySqmParam;
	}

	public SelectStatement getSqlAst() {
		return sqlAst;
	}

	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamsBySqmParam;
	}
}
