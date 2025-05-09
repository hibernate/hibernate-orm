/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.emptyList;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingSqmFunction<T> extends SqmFunction<T> {
	private final @Nullable ReturnableType<T> impliedResultType;
	private final @Nullable ArgumentsValidator argumentsValidator;
	private final FunctionReturnTypeResolver returnTypeResolver;
	private final FunctionRenderer renderer;
	private @Nullable ReturnableType<?> resultType;

	public SelfRenderingSqmFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super( name, descriptor,
				impliedResultType == null ? null
						: impliedResultType.resolveExpressible( nodeBuilder ),
				arguments, nodeBuilder );
		this.renderer = renderer;
		this.impliedResultType = impliedResultType;
		this.argumentsValidator = argumentsValidator;
		this.returnTypeResolver = returnTypeResolver;
	}

	@Override
	public SelfRenderingSqmFunction<T> copy(SqmCopyContext context) {
		final SelfRenderingSqmFunction<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( getArguments().size() );
		for ( SqmTypedNode<?> argument : getArguments() ) {
			arguments.add( argument.copy( context ) );
		}
		final SelfRenderingSqmFunction<T> expression = context.registerCopy(
				this,
				new SelfRenderingSqmFunction<>(
						getFunctionDescriptor(),
						getFunctionRenderer(),
						arguments,
						getImpliedResultType(),
						getArgumentsValidator(),
						getReturnTypeResolver(),
						nodeBuilder(),
						getFunctionName()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public FunctionRenderer getFunctionRenderer() {
		return renderer;
	}

	protected @Nullable ReturnableType<T> getImpliedResultType() {
		return impliedResultType;
	}

	protected @Nullable ArgumentsValidator getArgumentsValidator() {
		return argumentsValidator;
	}

	protected FunctionReturnTypeResolver getReturnTypeResolver() {
		return returnTypeResolver;
	}

	protected List<SqlAstNode> resolveSqlAstArguments(List<? extends SqmTypedNode<?>> sqmArguments, SqmToSqlAstConverter walker) {
		if ( sqmArguments.isEmpty() ) {
			return emptyList();
		}
		else {
			final FunctionArgumentTypeResolver argumentTypeResolver = getArgumentTypeResolver();
			return argumentTypeResolver == null
					? collectArguments( sqmArguments, walker )
					: resolveArguments( sqmArguments, walker, argumentTypeResolver );
		}
	}

	private List<SqlAstNode> resolveArguments(
			List<? extends SqmTypedNode<?>> sqmArguments,
			SqmToSqlAstConverter walker,
			FunctionArgumentTypeResolver argumentTypeResolver) {
		final FunctionArgumentTypeResolverTypeAccess typeAccess =
				new FunctionArgumentTypeResolverTypeAccess( walker, this, argumentTypeResolver );
		final List<SqlAstNode> sqlAstArguments = new ArrayList<>( sqmArguments.size() );
		for ( int i = 0, size = sqmArguments.size(); i < size; i++ ) {
			typeAccess.argumentIndex = i;
			sqlAstArguments.add( (SqlAstNode) walker.visitWithInferredType( sqmArguments.get( i ), typeAccess ) );
		}
		return sqlAstArguments;
	}

	private static List<SqlAstNode> collectArguments(
			List<? extends SqmTypedNode<?>> sqmArguments,
			SqmToSqlAstConverter walker) {
		final List<SqlAstNode> sqlAstArguments = new ArrayList<>( sqmArguments.size() );
		for ( int i = 0, size = sqmArguments.size(); i < size; i++ ) {
			sqlAstArguments.add( (SqlAstNode) sqmArguments.get( i ).accept( walker ) );
		}
		return sqlAstArguments;
	}

	private FunctionArgumentTypeResolver getArgumentTypeResolver() {
		return getFunctionDescriptor() instanceof AbstractSqmFunctionDescriptor functionDescriptor
				? functionDescriptor.getArgumentTypeResolver()
				: null;
	}

	@Override
	public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
		final @Nullable ReturnableType<?> resultType = resolveResultType( walker );
		final List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
		final ArgumentsValidator validator = argumentsValidator;
		if ( validator != null ) {
			validator.validateSqlTypes( arguments, getFunctionName() );
		}
		return new SelfRenderingFunctionSqlAstExpression<>(
				getFunctionName(),
				getFunctionRenderer(),
				arguments,
				resultType,
				resultType == null ? null : getMappingModelExpressible( walker, resultType, arguments )
		);
	}

	public @Nullable SqmExpressible<T> getNodeType() {
		final SqmExpressible<T> nodeType = super.getNodeType();
		if ( nodeType == null ) {
			final NodeBuilder nodeBuilder = nodeBuilder();
			final ReturnableType<?> resultType =
					determineResultType( null, nodeBuilder.getTypeConfiguration() );
			if ( resultType == null ) {
				return null;
			}
			else {
				final SqmExpressible<?> expressibleType = resultType.resolveExpressible( nodeBuilder );
				setExpressibleType( expressibleType );
				return super.getNodeType();
			}
		}
		else {
			return nodeType;
		}
	}

	public @Nullable ReturnableType<?> resolveResultType(SqmToSqlAstConverter walker) {
		if ( resultType == null ) {
			resultType = determineResultType( walker, walker.getCreationContext().getTypeConfiguration() );
			if ( resultType != null ) {
				setExpressibleType( resultType.resolveExpressible( nodeBuilder() ) );
			}
		}
		return resultType;
	}

	protected @Nullable ReturnableType<?> determineResultType(
			SqmToSqlAstConverter converter,
			TypeConfiguration typeConfiguration) {
		return returnTypeResolver.resolveFunctionReturnType(
				impliedResultType,
				converter,
				getArguments(),
				typeConfiguration
		);
	}

	protected MappingModelExpressible<?> getMappingModelExpressible(
			SqmToSqlAstConverter walker,
			ReturnableType<?> resultType,
			List<SqlAstNode> arguments) {

		if ( resultType instanceof MappingModelExpressible<?> mappingModelExpressible ) {
			// here we have a BasicType, which can be cast
			// directly to BasicValuedMapping
			return mappingModelExpressible;
		}
		else {
			// here we have something that is not a BasicType,
			// and we have no way to get a BasicValuedMapping
			// from it directly
			final MappingMetamodelImplementor mappingMetamodel =
					walker.getCreationContext().getMappingMetamodel();
			return returnTypeResolver.resolveFunctionReturnType(
					() -> {
						try {
							final MappingModelExpressible<?> expressible =
									mappingMetamodel.resolveMappingExpressible( getNodeType(),
											walker.getFromClauseAccess()::getTableGroup );
							return (BasicValuedMapping) expressible;
						}
						catch (Exception e) {
							return null; // this works at least approximately
						}
					},
					arguments
			);
		}
	}

	private static class FunctionArgumentTypeResolverTypeAccess implements Supplier<MappingModelExpressible<?>> {

		private final SqmToSqlAstConverter converter;
		private final SqmFunction<?> function;
		private final FunctionArgumentTypeResolver argumentTypeResolver;
		private int argumentIndex;

		public FunctionArgumentTypeResolverTypeAccess(
				SqmToSqlAstConverter converter,
				SqmFunction<?> function,
				FunctionArgumentTypeResolver argumentTypeResolver) {
			this.converter = converter;
			this.function = function;
			this.argumentTypeResolver = argumentTypeResolver;
		}

		@Override
		public MappingModelExpressible<?> get() {
			return argumentTypeResolver.resolveFunctionArgumentType( function.getArguments(), argumentIndex, converter );
		}
	}

	@Override
	// TODO: override on all subtypes
	public boolean equals(Object other) {
		return other instanceof SelfRenderingSqmAggregateFunction<?> that
			&& Objects.equals( this.toHqlString(), that.toHqlString() );
	}

	@Override
	public int hashCode() {
		return toHqlString().hashCode();
	}
}
