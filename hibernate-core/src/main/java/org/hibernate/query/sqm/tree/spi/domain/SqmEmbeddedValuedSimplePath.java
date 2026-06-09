/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.TreatException;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Type.PersistenceType.EMBEDDABLE;

/**
 * @author Steve Ebersole
 */
public class SqmEmbeddedValuedSimplePath<T>
		extends AbstractSqmSimplePath<T>
		implements SqmBindableType<T> {

	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
		assert referencedPathSource.getPathType() instanceof EmbeddableDomainType;
	}

	@SuppressWarnings("unused")
	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
		assert referencedPathSource.getPathType() instanceof EmbeddableDomainType;
	}

	@Override
	public SqmEmbeddedValuedSimplePath<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmEmbeddedValuedSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						getModel(),
						lhsCopy,
						getExplicitAlias(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Override
	public @Nonnull SqmBindableType<T> getExpressible() {
		return this;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return EMBEDDABLE;
	}

	@Override
	public @Nullable SqmDomainType<T> getSqmType() {
		return getResolvedModel().getSqmType();
	}

	@Override
	public SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		final var sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEmbeddableValuedPath( this );
	}


	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull Class<S> treatJavaType) {
		return getTreatedPath( nodeBuilder().getDomainModel().embeddable( treatJavaType ) );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		throw new TreatException( "Embeddable paths cannot be TREAT-ed to an entity type" );
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return super.getExpressible().getExpressibleJavaType();
	}

	@Override
	public @Nonnull Class<T> getJavaType() {
		if ( getModel() instanceof SqmPersistentAttribute<?, ?> attribute ) {
			// Generic-specialized embedded attributes keep the concrete model for navigation,
			// but expose the declaring member's Java type through the criteria path.
			return (Class<T>) attribute.getJavaType();
		}
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return super.getExpressible().getRelationalJavaType();
	}
}
