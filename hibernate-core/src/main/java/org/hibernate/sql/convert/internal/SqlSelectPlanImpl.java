/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.internal;

import java.util.List;

import org.hibernate.sql.tree.SelectStatement;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.spi.SqlSelectPlan;

/**
 * @author Steve Ebersole
 */
public class SqlSelectPlanImpl implements SqlSelectPlan {
	private final SelectStatement selectQuery;
	private final List<Return> queryReturns;

	public SqlSelectPlanImpl(
			SelectStatement selectQuery,
			List<Return> queryReturns) {
		this.selectQuery = selectQuery;
		this.queryReturns = queryReturns;
	}

	@Override
	public SelectStatement getSqlSelectAst() {
		return selectQuery;
	}

	@Override
	public List<Return> getQueryReturns() {
		return queryReturns;
	}
}
