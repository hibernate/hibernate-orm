/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaFunctionRoot;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.NavigablePath;


/**
 * @author Christian Beikov
 */
@Incubating
public class SqmFunctionRoot<E> extends SqmRoot<E> implements JpaFunctionRoot<E> {

	private final SqmSetReturningFunction<E> function;

	public SqmFunctionRoot(SqmSetReturningFunction<E> function, @Nullable String alias) {
		this(
				SqmCreationHelper.buildRootNavigablePath( "<<derived>>", alias ),
				function,
				function.getType(),
				alias
		);
	}

	protected SqmFunctionRoot(
			NavigablePath navigablePath,
			SqmSetReturningFunction<E> function,
			SqmPathSource<E> pathSource,
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

	@Override
	public SqmFunctionRoot<E> copy(SqmCopyContext context) {
		final SqmFunctionRoot<E> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmFunctionRoot<E> path = context.registerCopy(
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

	@Override
	public SqmSetReturningFunction<E> getFunction() {
		return function;
	}

	@Override
	public SqmPath<Long> index() {
		final SqmPathSource<?> pathSource =
				function.getType().getSubPathSource( CollectionPart.Nature.INDEX.getName() );
		//noinspection unchecked
		final SqmPathSource<Long> indexPathSource = (SqmPathSource<Long>) pathSource;
		return resolvePath( indexPathSource.getPathName(), indexPathSource );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRootFunction( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public SqmEntityDomainType<E> getModel() {
		throw new UnsupportedOperationException( "Function root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Override
	public String getEntityName() {
		throw new UnsupportedOperationException( "Function root does not have an entity type. Use getReferencedPathSource() instead." );
	}

	@Override
	public SqmPathSource<E> getResolvedModel() {
		return getReferencedPathSource();
	}

	@Override
	public SqmCorrelatedRoot<E> createCorrelation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends E> SqmTreatedFrom<E, E, S>  treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException( "Function roots can not be treated" );
	}

	@Override
	public boolean deepEquals(SqmFrom<?, ?> object) {
		return super.deepEquals( object )
			&& function.equals( ((SqmFunctionRoot<?>) object).function );
	}

	@Override
	public boolean isDeepCompatible(SqmFrom<?, ?> object) {
		return super.isDeepCompatible( object )
			&& function.isCompatible( ((SqmFunctionRoot<?>) object).function );
	}
}
