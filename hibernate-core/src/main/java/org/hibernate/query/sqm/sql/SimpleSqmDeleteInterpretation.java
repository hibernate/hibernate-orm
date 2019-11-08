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
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class SimpleSqmDeleteInterpretation implements SqmInterpretation {
	private final DeleteStatement sqlAst;
	private final Map<SqmParameter, List<JdbcParameter>> jdbcParamMap;

	public SimpleSqmDeleteInterpretation(
			DeleteStatement sqlAst,
			Map<SqmParameter, List<JdbcParameter>> jdbcParamMap) {
		this.sqlAst = sqlAst;
		this.jdbcParamMap = jdbcParamMap;
	}

	@Override
	public DeleteStatement getSqlAst() {
		return sqlAst;
	}

	@Override
	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamMap;
	}
}
