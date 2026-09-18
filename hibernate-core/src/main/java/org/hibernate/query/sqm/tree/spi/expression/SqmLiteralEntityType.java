/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;

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

	public SqmLiteralEntityType(@Nonnull SqmEntityDomainType<T> entityType, @Nonnull NodeBuilder nodeBuilder) {
		super( getDiscriminatorType( entityType, nodeBuilder ), nodeBuilder );
		this.entityType = entityType;
	}

	@Nonnull
	@Override
	public SqmLiteralEntityType<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
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
	@Nonnull
	public SqmEntityDomainType<T> getNodeType() {
		return entityType;
	}

	@Override
	public void internalApplyInferableType(@Nullable SqmBindableType<?> type) {
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitEntityTypeLiteralExpression( this );
	}

//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState) {
//		throw new SemanticException( "Selecting an entity type is not allowed. An entity type expression can be used to restrict query polymorphism ");
//		// todo (6.0) : but could be ^^ - consider adding support for this (returning Class)
//	}


	@Nonnull
	@Override
	public String asLoggableText() {
		return "TYPE(" + entityType + ")";
	}

	@Nonnull
	@Override
	public SemanticPathPart resolvePathPart(
			@Nonnull String name,
			boolean isTerminal,
			@Nonnull SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}

	@Nonnull
	@Override
	public SqmPath<?> resolveIndexedAccess(
			@Nonnull SqmExpression<?> selector,
			boolean isTerminal,
			@Nonnull SqmCreationState creationState) {
		throw new HqlInterpretationException( "Cannot dereference an entity name" );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( entityType.getName() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmLiteralEntityType<?> that
			&& Objects.equals( this.entityType.getName(), that.entityType.getName() );
	}

	@Override
	public int hashCode() {
		return entityType.getName().hashCode();
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
