/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

import java.util.Objects;

import static org.hibernate.persister.entity.DiscriminatorHelper.getDiscriminatorType;

/**
 * Represents a reference to an entity type as a literal.
 * In a restriction like {@code where TYPE(e) = SomeType},
 * the token {@code SomeType} is used to restrict query
 * polymorphism. The {@code TYPE} operator returns the
 * exact concrete type of the argument.
 *
 * @author Steve Ebersole
 */
public class SqmLiteralEntityType<T>
		extends AbstractSqmExpression<T>
		implements SqmSelectableNode<T>, SemanticPathPart {
	private final SqmEntityDomainType<T> entityType;

	public SqmLiteralEntityType(SqmEntityDomainType<T> entityType, NodeBuilder nodeBuilder) {
		super( getDiscriminatorType( entityType, nodeBuilder ), nodeBuilder );
		this.entityType = entityType;
	}

	@Override
	public SqmLiteralEntityType<T> copy(SqmCopyContext context) {
		final SqmLiteralEntityType<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLiteralEntityType<T> expression =
				context.registerCopy( this,
						new SqmLiteralEntityType<>( entityType, nodeBuilder() ) );
		copyTo( expression, context );
		return expression;
	}

	@Override
	public SqmEntityDomainType<T> getNodeType() {
		return entityType;
	}

	@Override
	public void internalApplyInferableType(SqmExpressible<?> type) {
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEntityTypeLiteralExpression( this );
	}

//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState) {
//		throw new SemanticException( "Selecting an entity type is not allowed. An entity type expression can be used to restrict query polymorphism ");
//		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
//	}


	@Override
	public String asLoggableText() {
		return "TYPE(" + entityType + ")";
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( entityType.getName() );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmLiteralEntityType<?> that
			&& Objects.equals( this.entityType.getName(), that.entityType.getName() );
	}

	@Override
	public int hashCode() {
		return entityType.getName().hashCode();
	}
}
