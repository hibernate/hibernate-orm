package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
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
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<T> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			@Nonnull NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmBasicValuedSimplePath(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<T> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			@Nullable String explicitAlias,
			@Nonnull NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Nonnull
	@Override
	public SqmBasicValuedSimplePath<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final var lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				createCopy(
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

	@Nonnull
	protected SqmBasicValuedSimplePath<T> createCopy(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPathSource<T> referencedPathSource,
			@Nullable SqmPath<?> lhs,
			@Nullable String explicitAlias,
			@Nonnull NodeBuilder nodeBuilder) {
		return new SqmBasicValuedSimplePath<>(
				navigablePath,
				referencedPathSource,
				lhs,
				explicitAlias,
				nodeBuilder
		);
	}

	@Override
	public @Nonnull SqmBindableType<T> getExpressible() {
		return this;
	}

	@Override
	@Nonnull
	public PersistenceType getPersistenceType() {
		return BASIC;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SemanticPathPart

	@Nonnull
	@Override
	public SqmPath<?> resolvePathPart(
			@Nonnull String name,
			boolean isTerminal,
			@Nonnull SqmCreationState creationState) {
		throw new UnknownPathException(
				String.format(
						"Could not interpret attribute '%s' of basic-valued path '%s'",
						name, getNavigablePath()
				)
		);
	}

	@Nonnull
	@Override
	public SqmPath<?> resolveIndexedAccess(
			@Nonnull SqmExpression<?> selector,
			boolean isTerminal,
			@Nonnull SqmCreationState creationState) {
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

	@Nonnull
	private SelfRenderingSqmFunction<?> getIndexFunction(
			@Nonnull SqmExpression<?> selector, @Nonnull SqmDomainType<T> sqmPathType, @Nonnull QueryEngine queryEngine) {
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

	private @Nullable Class<?> getJavaTypeClass(@Nonnull SqmDomainType<T> sqmPathType) {
		final SqmBindableType<T> expressible = nodeBuilder().resolveExpressible( sqmPathType );
		return expressible == null ? null : expressible.getRelationalJavaType().getJavaTypeClass();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath

	@Override
	public @Nonnull BasicJavaType<T> getJavaTypeDescriptor() {
		return (BasicJavaType<T>) super.getJavaTypeDescriptor();
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T,S> treatAs(@Nonnull Class<S> treatJavaType) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public @Nonnull Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Nonnull
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

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitBasicValuedPath( this );
	}

	@Nonnull
	@Override
	public JavaType<?> getRelationalJavaType() {
		return super.getExpressible().getRelationalJavaType();
	}
}
