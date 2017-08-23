/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.tree.TreeException;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * Represents an reference to an entity type as a literal.  This is the JPA
 * terminology for cases when we have something like: {@code ... where TYPE(e) = SomeType}.
 * The token {@code SomeType} is an "entity type literal".
 *
 * An entity type expression can be used to restrict query polymorphism. The TYPE operator returns the exact type of the argument.
 *
 * @author Steve Ebersole
 */
public class EntityTypeLiteralSqmExpression implements SqmExpression {
	private final EntityValuedExpressableType entityType;

	public EntityTypeLiteralSqmExpression(EntityValuedExpressableType entityType) {
		this.entityType = entityType;
	}

	@Override
	public EntityValuedExpressableType getExpressionType() {
		return entityType;
	}

	@Override
	public EntityValuedExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityTypeLiteralExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "TYPE(" + entityType + ")";
	}

	@Override
	public QueryResult createQueryResult(
			Expression expression,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		throw new TreeException("Selecting an entity type is not allowed. An entity type expression can be used to restrict query polymorphism ");
		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
	}


}
