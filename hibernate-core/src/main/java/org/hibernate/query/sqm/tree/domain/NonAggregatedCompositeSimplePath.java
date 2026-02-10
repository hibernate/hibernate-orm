/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.TreatException;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import jakarta.persistence.metamodel.EntityType;

/**
 * @author Andrea Boriero
 */
public class NonAggregatedCompositeSimplePath<T> extends SqmEntityValuedSimplePath<T> {

	public NonAggregatedCompositeSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );

		assert referencedPathSource.getPathType() instanceof EntityType;
	}

	@Override
	public NonAggregatedCompositeSimplePath<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final var lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new NonAggregatedCompositeSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						getModel(),
						lhsCopy,
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitNonAggregatedCompositeValuedPath( this );
	}

	@Override
	public <S extends T> SqmTreatedEntityValuedSimplePath<T, S> treatAs(Class<S> treatJavaType) {
		throw new TreatException( "Non-aggregate composite paths cannot be TREAT-ed" );
	}

	@Override
	public <S extends T> SqmTreatedSimplePath<T, S> treatAs(EntityDomainType<S> treatTarget) {
		throw new TreatException( "Non-aggregate composite paths cannot be TREAT-ed" );
	}

	@Override
	public <S extends T> SqmTreatedSimplePath<T, S> treatAs(Class<S> treatJavaType, @Nullable String alias) {
		throw new TreatException( "Non-aggregate composite paths cannot be TREAT-ed" );
	}

	@Override
	public <S extends T> SqmTreatedSimplePath<T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new TreatException( "Non-aggregate composite paths cannot be TREAT-ed" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		throw new TreatException( "Non-aggregate composite paths cannot be TREAT-ed" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		throw new TreatException( "Non-aggregate composite paths cannot be TREAT-ed" );
	}

}
