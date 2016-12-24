/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.internal;

import java.util.List;

import org.hibernate.sql.ast.SelectQuery;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.spi.SqmSelectInterpretation;

/**
 * @author Steve Ebersole
 */
public class SqmSelectInterpretationImpl implements SqmSelectInterpretation {
	private final SelectQuery selectQuery;
	private final List<Return> queryReturns;

	public SqmSelectInterpretationImpl(
			SelectQuery selectQuery,
			List<Return> queryReturns) {
		this.selectQuery = selectQuery;
		this.queryReturns = queryReturns;
	}

	@Override
	public SelectQuery getSqlSelectAst() {
		return selectQuery;
	}

	@Override
	public List<Return> getQueryReturns() {
		return queryReturns;
	}
}
