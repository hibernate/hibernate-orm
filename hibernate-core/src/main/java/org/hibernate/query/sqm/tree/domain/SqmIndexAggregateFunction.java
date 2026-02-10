/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class SqmIndexAggregateFunction<T> extends AbstractSqmSpecificPluralPartPath<T> {
	private final String functionName;
	private final @Nullable ReturnableType<T> returnableType;

	public SqmIndexAggregateFunction(SqmPluralValuedSimplePath<?> pluralDomainPath, String functionName) {
		//noinspection unchecked
		super(
				pluralDomainPath.getParentNavigablePath()
						.append( pluralDomainPath.getNavigablePath().getLocalName(), "{" + functionName + "-index}" ),
				pluralDomainPath,
				(SqmPluralPersistentAttribute<?, ?, ?>)
						pluralDomainPath.getReferencedPathSource(),
				(SqmPathSource<T>)
						( (SqmPluralPersistentAttribute<?, ?, ?>) pluralDomainPath.getReferencedPathSource() )
								.getIndexPathSource()
		);
		this.functionName = functionName;
		final var nodeBuilder = pluralDomainPath.nodeBuilder();
		final var type = switch ( functionName ) {
			case "sum" ->
					nodeBuilder.getSumReturnTypeResolver()
							.resolveFunctionReturnType(
									null,
									(SqmToSqlAstConverter) null,
									List.of( pluralDomainPath.get( CollectionPart.Nature.INDEX.getName() ) ),
									nodeBuilder.getTypeConfiguration()
							);
			case "avg" ->
					nodeBuilder.getAvgReturnTypeResolver()
							.resolveFunctionReturnType(
									null,
									(SqmToSqlAstConverter) null,
									List.of( pluralDomainPath.get( CollectionPart.Nature.INDEX.getName() ) ),
									nodeBuilder.getTypeConfiguration()
							);
			default -> null;
		};
		//noinspection unchecked
		returnableType = (ReturnableType<T>) type;
	}

	@Override
	public @NonNull SqmBindableType<T> getExpressible() {
		return returnableType == null
				? super.getExpressible()
				: castNonNull( nodeBuilder().resolveExpressible( returnableType ) );
	}

	@Override
	public @NonNull JavaType<T> getJavaTypeDescriptor() {
		return returnableType == null
				? super.getJavaTypeDescriptor()
				: returnableType.getExpressibleJavaType();
	}

	@Override
	public @NonNull JavaType<T> getNodeJavaType() {
		return returnableType == null ? super.getNodeJavaType() : returnableType.getExpressibleJavaType();
	}

	@Override
	public SqmIndexAggregateFunction<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final var path = context.registerCopy(
				this,
				new SqmIndexAggregateFunction<T>(
						getPluralDomainPath().copy( context ),
						functionName
				)
		);
		copyTo( path, context );
		return path;
	}

	public String getFunctionName() {
		return functionName;
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final var sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitIndexAggregateFunction( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( functionName ).append( "(" );
		getLhs().appendHqlString( hql, context );
		hql.append( ')' );
	}
}
