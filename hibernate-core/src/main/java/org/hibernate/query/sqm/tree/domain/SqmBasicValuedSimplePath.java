/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;
import static java.util.Arrays.asList;

/**
 * @author Steve Ebersole
 */
public class SqmBasicValuedSimplePath<T>
		extends AbstractSqmSimplePath<T>
		implements SqmBindableType<T> {
	public SqmBasicValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmBasicValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	public SqmBasicValuedSimplePath<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final var lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmBasicValuedSimplePath<>(
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
	public @NonNull SqmBindableType<T> getExpressible() {
		return this;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return BASIC;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SemanticPathPart

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnknownPathException(
				String.format(
						"Could not interpret attribute '%s' of basic-valued path '%s'",
						name, getNavigablePath()
				)
		);
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		final var pathRegistry =
				creationState.getCurrentProcessingState().getPathRegistry();
		final String alias = selector.toHqlString();
		final NavigablePath navigablePath =
				getParentNavigablePath().append( CollectionPart.Nature.ELEMENT.getName(), alias );
		final SqmFrom<?, ?> indexedPath = pathRegistry.findFromByPath( navigablePath );
		if ( indexedPath != null ) {
			return indexedPath;
		}
		else {
			final SqmFunctionPath<Object> path =
					new SqmFunctionPath<>(
							getIndexFunction(
									selector,
//									getNodeType().getPathType(),
									getReferencedPathSource().getPathType(),
									creationState.getCreationContext().getQueryEngine()
							)
					);
			pathRegistry.register( path );
			return path;
		}
	}

	private SelfRenderingSqmFunction<?> getIndexFunction(
			SqmExpression<?> selector, SqmDomainType<T> sqmPathType, QueryEngine queryEngine) {
		final SqmFunctionRegistry registry = queryEngine.getSqmFunctionRegistry();
		if ( sqmPathType instanceof BasicPluralType<?, ?> ) {
			return registry.getFunctionDescriptor( "array_get" )
					.generateSqmExpression(
							asList( this, selector ),
							null,
							queryEngine
					);
		}
		else if ( getJavaTypeClass( sqmPathType ) == String.class ) {
			return registry.getFunctionDescriptor( "substring" )
					.generateSqmExpression(
							asList( this, selector, nodeBuilder().literal( 1 ) ),
							nodeBuilder().getCharacterType(),
							queryEngine
					);
		}
		else {
			throw new UnsupportedOperationException( "Index access is only supported for basic plural and string types, but got: " + sqmPathType );
		}
	}

	private @Nullable Class<?> getJavaTypeClass(SqmDomainType<T> sqmPathType) {
		final SqmBindableType<T> expressible = nodeBuilder().resolveExpressible( sqmPathType );
		return expressible == null ? null : expressible.getRelationalJavaType().getJavaTypeClass();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath

	@Override
	public @NonNull BasicJavaType<T> getJavaTypeDescriptor() {
		return (BasicJavaType<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public <S extends T> SqmTreatedPath<T,S> treatAs(Class<S> treatJavaType) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType, @Nullable String alias) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public @NonNull Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return super.getExpressible().getExpressibleJavaType();
	}

	@Override
	public @Nullable SqmDomainType<T> getSqmType() {
		return getResolvedModel().getSqmType();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Visitation

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitBasicValuedPath( this );
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return super.getExpressible().getRelationalJavaType();
	}
}
