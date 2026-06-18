/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmEmbeddableDomainType;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;

import java.util.Objects;

import static org.hibernate.persister.entity.DiscriminatorHelper.getDiscriminatorType;

/**
 * Represents a reference to an embeddable type as a literal.
 *
 * @author Marco Belladelli
 */
public class SqmLiteralEmbeddableType<T>
		extends AbstractSqmExpression<T>
		implements SqmSelectableNode<T>, SemanticPathPart {
	final SqmEmbeddableDomainType<T> embeddableDomainType;

	public SqmLiteralEmbeddableType(
			SqmEmbeddableDomainType<T> embeddableDomainType,
			NodeBuilder nodeBuilder) {
		super( getDiscriminatorType( embeddableDomainType, nodeBuilder), nodeBuilder );
		this.embeddableDomainType = embeddableDomainType;
	}

	public EmbeddableDomainType<T> getEmbeddableDomainType() {
		return embeddableDomainType;
	}

	@Override
	public SqmLiteralEmbeddableType<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLiteralEmbeddableType<T> expression =
				context.registerCopy( this,
						new SqmLiteralEmbeddableType<>( embeddableDomainType, nodeBuilder() ) );
		copyTo( expression, context );
		return expression;
	}

	@Override
	public void internalApplyInferableType(@Nullable SqmBindableType<?> type) {
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEmbeddableTypeLiteralExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "TYPE(" + embeddableDomainType + ")";
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an embeddable name" );
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an embeddable name" );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( embeddableDomainType.getTypeName() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmLiteralEmbeddableType<?> that
			&& Objects.equals( embeddableDomainType.getTypeName(), that.embeddableDomainType.getTypeName() );
	}

	@Override
	public int hashCode() {
		return embeddableDomainType.getTypeName().hashCode();
	}

	@Override
	public boolean isCompatible(Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
