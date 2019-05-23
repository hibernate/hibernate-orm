/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * Entity type expression based on a parameter - `TYPE( :someParam )`
 *
 * @author Steve Ebersole
 */
public class SqmParameterizedEntityType<T> extends AbstractSqmExpression<T> implements DomainResultProducer {
	private final SqmParameter parameterExpression;

	public SqmParameterizedEntityType(SqmParameter<T> parameterExpression, NodeBuilder nodeBuilder) {
		super( parameterExpression.getAnticipatedType(), nodeBuilder );
		this.parameterExpression = parameterExpression;
	}

	@Override
	public EntityValuedExpressableType<T> getNodeType() {
		//noinspection unchecked
		return (EntityValuedExpressableType<T>) parameterExpression.getNodeType();
	}

	@Override
	public void internalApplyInferableType(SqmExpressable<?> type) {
		setExpressableType( type );

		//noinspection unchecked
		parameterExpression.applyInferableType( type );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitParameterizedEntityTypeExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "TYPE(" + parameterExpression.asLoggableText() + ")";
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException( "At the moment, selection of an entity's type as a QueryResult is not supported" );
		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
	}

}
