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
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class SqmInsertSelectTranslation {
	private final InsertSelectStatement sqlAst;
	private final Map<SqmParameter, List<JdbcParameter>> jdbcParamMap;

	public SqmInsertSelectTranslation(
			InsertSelectStatement sqlAst,
			Map<SqmParameter, List<JdbcParameter>> jdbcParamMap) {
		this.sqlAst = sqlAst;
		this.jdbcParamMap = jdbcParamMap;
	}

	public InsertSelectStatement getSqlAst() {
		return sqlAst;
	}

	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamMap;
	}
}
