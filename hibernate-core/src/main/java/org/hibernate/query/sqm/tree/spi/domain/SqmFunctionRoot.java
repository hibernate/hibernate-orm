/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaFunctionRoot;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.spi.NavigablePath;


/**
 * @author Christian Beikov
 */
@Incubating(since = "6.2")
public class SqmFunctionRoot<E> extends SqmRoot<E> implements JpaFunctionRoot<E> {

	private final SqmSetReturningFunction<E> function;

	public SqmFunctionRoot(@Nonnull SqmSetReturningFunction<E> function, @Nullable String alias) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<derived>>", alias ),
				function,
				function.getType(),
				alias
		);
	}

	protected SqmFunctionRoot(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmSetReturningFunction<E> function,
			@Nonnull SqmPathSource<E> pathSource,
			@Nullable String alias) {
		super(
				navigablePath,
				pathSource,
				alias,
				true,
				function.nodeBuilder()
		);
		this.function = function;
	}

	@Nonnull
	@Override
	public SqmFunctionRoot<E> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var path = context.registerCopy(
				this,
				new SqmFunctionRoot<>(
						getNavigablePath(),
						getFunction().copy( context ),
						getReferencedPathSource(),
						getExplicitAlias()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	@Override
	public SqmSetReturningFunction<E> getFunction() {
		return function;
	}

	@Nonnull
	@Override
	public SqmPath<Long> index() {
		final SqmPathSource<?> pathSource =
				function.getType().getSubPathSource( CollectionPart.Nature.INDEX.getName() );
		//noinspection unchecked
		final SqmPathSource<Long> indexPathSource = (SqmPathSource<Long>) pathSource;
		return resolvePath( indexPathSource.getPathName(), indexPathSource );
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitRootFunction( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Nonnull
	@Override
	public SqmEntityDomainType<E> getModel() {
		throw new UnsupportedOperationException( "Function root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Nonnull
	@Override
	public String getEntityName() {
		throw new UnsupportedOperationException( "Function root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Nonnull
	@Override
	public SqmPathSource<E> getResolvedModel() {
		return getReferencedPathSource();
	}

	@Override
	@Nonnull
	public SqmCorrelatedRoot<E> createCorrelation() {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public <S extends E> SqmTreatedFrom<E, E, S>  treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException( "Function roots can not be treated" );
	}

	@Override
	public boolean deepEquals(@Nonnull SqmFrom<?, ?> object) {
		return super.deepEquals( object )
			&& function.equals( ((SqmFunctionRoot<?>) object).function );
	}

	@Override
	public boolean isDeepCompatible(@Nonnull SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& function.isCompatible( ((SqmFunctionRoot<?>) object).function );
	}
}
