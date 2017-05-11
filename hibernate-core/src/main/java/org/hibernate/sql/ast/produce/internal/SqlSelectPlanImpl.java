/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.internal;

import java.util.List;

import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.spi.SqlSelectPlan;

/**
 * @author Steve Ebersole
 */
public class SqlSelectPlanImpl implements SqlSelectPlan {
	private final SelectStatement selectQuery;
	private final List<QueryResult> queryReturns;

	public SqlSelectPlanImpl(
			SelectStatement selectQuery,
			List<QueryResult> queryReturns) {
		this.selectQuery = selectQuery;
		this.queryReturns = queryReturns;
	}

	@Override
	public SelectStatement getSqlAstSelectStatement() {
		return selectQuery;
	}

	@Override
	public List<QueryResult> getQueryResults() {
		return queryReturns;
	}
}
