/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.HqlInterpretationException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectableNode;
import org.hibernate.type.BasicType;

import java.util.Objects;

public class SqmAnyDiscriminatorValue<T> extends AbstractSqmExpression<T>
		implements SqmSelectableNode<T>, SemanticPathPart {

	private final EntityDomainType value;
	private final BasicType domainType;
	private final String pathName;

	public SqmAnyDiscriminatorValue(
			@Nonnull String pathName,
			@Nonnull EntityDomainType entityValue,
			@Nonnull BasicType<T> domainType,
			@Nonnull NodeBuilder nodeBuilder) {
		super( domainType, nodeBuilder );
		this.value = entityValue;
		this.pathName = pathName;
		this.domainType = domainType;
	}

	@Nonnull
	public BasicType<T> getDomainType(){
		return domainType;
	}

	@Nonnull
	@Override
	public SqmAnyDiscriminatorValue<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmAnyDiscriminatorValue<T> expression = context.registerCopy(
				this,
				new SqmAnyDiscriminatorValue<>(
						pathName,
						value,
						domainType,
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitAnyDiscriminatorTypeValueExpression( this );
	}

	@Nonnull
	public EntityDomainType getEntityValue() {
		return value;
	}

	@Nonnull
	public String getPathName() {
		return pathName;
	}

	@Nonnull
	@Override
	public String asLoggableText() {
		return getEntityValue().getName();
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
		hql.append( getEntityValue().getName() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmAnyDiscriminatorValue<?> that
			&& Objects.equals( this.value.getName(), that.value.getName() )
			&& Objects.equals( this.pathName, that.pathName );
	}

	@Override
	public int hashCode() {
		int result = value.getName().hashCode();
		result = 31 * result + pathName.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return object instanceof SqmAnyDiscriminatorValue<?> that
			&& Objects.equals( this.value.getName(), that.value.getName() )
			&& Objects.equals( this.pathName, that.pathName );
	}

	@Override
	public int cacheHashCode() {
		int result = value.getName().hashCode();
		result = 31 * result + pathName.hashCode();
		return result;
	}
}
