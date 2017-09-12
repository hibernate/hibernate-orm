/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * Represents the {@code SIZE()} function.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
public class SqmCollectionSize implements SqmExpression {
	private final SqmPluralAttributeReference pluralAttributeBinding;
	private final BasicValuedExpressableType sizeType;

	public SqmCollectionSize(SqmPluralAttributeReference pluralAttributeBinding, BasicValuedExpressableType sizeType) {
		this.pluralAttributeBinding = pluralAttributeBinding;
		this.sizeType = sizeType;
	}

	public SqmPluralAttributeReference getPluralAttributeBinding() {
		return pluralAttributeBinding;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return sizeType;
	}

	@Override
	public BasicValuedExpressableType getInferableType() {
		return getExpressableType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttributeSizeFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "SIZE(" + pluralAttributeBinding.asLoggableText() + ")";
	}

	@Override
	public QueryResult createQueryResult(
			Expression expression, String resultVariable, QueryResultCreationContext creationContext) {
		return new ScalarQueryResultImpl(
				resultVariable,
				creationContext.getSqlSelectionResolver().resolveSqlSelection( expression ),
				getExpressableType()
		);
	}
}
