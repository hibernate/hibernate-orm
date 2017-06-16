/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCollection;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public class QueryResultCollectionImpl extends AbstractCollectionReference implements QueryResultCollection {
	public QueryResultCollectionImpl(
			NavigableReference selectedExpression,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		super( selectedExpression, resultVariable );
	}

	@Override
	public Expression getSelectedExpression() {
		return getNavigableReference();
	}

	@Override
	public String getResultVariable() {
		return super.getResultVariable();
	}

	@Override
	public Class getReturnedJavaType() {
		return getNavigableReference().getNavigable().getJavaType();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		throw new NotYetImplementedException(  );
	}
}
