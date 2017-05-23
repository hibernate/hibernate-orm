/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCollection;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public class QueryResultCollectionImpl extends AbstractCollectionReference implements QueryResultCollection {
	private final String resultVariable;

	public QueryResultCollectionImpl(NavigableReference navigableReference, String resultVariable) {
		super( navigableReference, true );
		this.resultVariable = resultVariable;
	}

	@Override
	public Expression getSelectedExpression() {
		return getNavigableReference();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public Class getReturnedJavaType() {
		return getNavigableReference().getNavigable().getJavaType();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return null;
	}
}
