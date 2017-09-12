/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class SqmNamedParameter implements SqmParameter {
	private final String name;
	private final boolean canBeMultiValued;
	private ExpressableType expressableType;

	public SqmNamedParameter(String name, boolean canBeMultiValued) {
		this.name = name;
		this.canBeMultiValued = canBeMultiValued;
	}

	public SqmNamedParameter(String name, boolean canBeMultiValued, ExpressableType expressableType) {
		this.name = name;
		this.canBeMultiValued = canBeMultiValued;
		this.expressableType = expressableType;
	}

	@Override
	public ExpressableType getExpressableType() {
		return expressableType;
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public void impliedType(ExpressableType expressableType) {
		if ( expressableType != null ) {
			this.expressableType = expressableType;
		}
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitNamedParameterExpression( this );
	}

	@Override
	public String asLoggableText() {
		return ":" + getName();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Integer getPosition() {
		return null;
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return canBeMultiValued;
	}

	@Override
	public ExpressableType getAnticipatedType() {
		return getExpressableType();
	}

	@Override
	public QueryResult createQueryResult(
			Expression expression,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return new ScalarQueryResultImpl( resultVariable,
										  creationContext.getSqlSelectionResolver().resolveSqlSelection( expression ),
										  (BasicValuedExpressableType) getExpressableType()
		);
	}
}
