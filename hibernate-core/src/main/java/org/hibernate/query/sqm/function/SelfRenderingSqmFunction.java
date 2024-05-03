/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.ReturnableType;
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
	private final FunctionRenderingSupport renderingSupport;
	private final FunctionRenderer renderer;
	private @Nullable ReturnableType<?> resultType;

	/**
	 * @deprecated Use {@link #SelfRenderingSqmFunction(SqmFunctionDescriptor, FunctionRenderer, List, ReturnableType, ArgumentsValidator, FunctionReturnTypeResolver, NodeBuilder, String)} instead
	 */
	@Deprecated(forRemoval = true)
	public SelfRenderingSqmFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderingSupport renderingSupport,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super( name, descriptor, impliedResultType, arguments, nodeBuilder );
		this.renderingSupport = renderingSupport;
		this.renderer = renderingSupport::render;
		this.impliedResultType = impliedResultType;
		this.argumentsValidator = argumentsValidator;
		this.returnTypeResolver = returnTypeResolver;
	}

	public SelfRenderingSqmFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super( name, descriptor, impliedResultType, arguments, nodeBuilder );
		this.renderingSupport = renderer;
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

	/**
	 * @deprecated Use {@link #getFunctionRenderer()} instead
	 */
	@Deprecated(forRemoval = true)
	public FunctionRenderingSupport getRenderingSupport() {
		return renderingSupport;
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

		final FunctionArgumentTypeResolver argumentTypeResolver;
		if ( getFunctionDescriptor() instanceof AbstractSqmFunctionDescriptor ) {
			argumentTypeResolver = ( (AbstractSqmFunctionDescriptor) getFunctionDescriptor() ).getArgumentTypeResolver();
		}
		else {
			argumentTypeResolver = null;
		}
		if ( argumentTypeResolver == null ) {
			final ArrayList<SqlAstNode> sqlAstArguments = new ArrayList<>( sqmArguments.size() );
			for ( int i = 0; i < sqmArguments.size(); i++ ) {
				sqlAstArguments.add(
						(SqlAstNode) sqmArguments.get( i ).accept( walker )
				);
			}
			return sqlAstArguments;
		}
		final FunctionArgumentTypeResolverTypeAccess typeAccess = new FunctionArgumentTypeResolverTypeAccess(
				walker,
				this,
				argumentTypeResolver
		);
		final ArrayList<SqlAstNode> sqlAstArguments = new ArrayList<>( sqmArguments.size() );
		for ( int i = 0; i < sqmArguments.size(); i++ ) {
			typeAccess.argumentIndex = i;
			sqlAstArguments.add(
					(SqlAstNode) walker.visitWithInferredType( sqmArguments.get( i ), typeAccess )
			);
		}
		return sqlAstArguments;
	}

	@Override
	public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
		final @Nullable ReturnableType<?> resultType = resolveResultType( walker );
		final List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
		final ArgumentsValidator validator = argumentsValidator;
		if ( validator != null ) {
			validator.validateSqlTypes( arguments, getFunctionName() );
		}
		return new SelfRenderingFunctionSqlAstExpression(
				getFunctionName(),
				getFunctionRenderer(),
				arguments,
				resultType,
				resultType == null ? null : getMappingModelExpressible( walker, resultType, arguments )
		);
	}

	public @Nullable SqmExpressible<T> getNodeType() {
		SqmExpressible<T> nodeType = super.getNodeType();
		if ( nodeType == null ) {
			//noinspection unchecked
			nodeType = (SqmExpressible<T>) determineResultType( null, nodeBuilder().getTypeConfiguration() );
			setExpressibleType( nodeType );
		}

		return nodeType;
	}

	@Deprecated(forRemoval = true)
	protected @Nullable ReturnableType<?> resolveResultType(TypeConfiguration typeConfiguration) {
		return resolveResultType( () -> null, typeConfiguration );
	}

	public @Nullable ReturnableType<?> resolveResultType(SqmToSqlAstConverter walker) {
		if ( resultType == null ) {
			resultType = determineResultType(
					walker,
					walker.getCreationContext().getMappingMetamodel().getTypeConfiguration()
			);
			setExpressibleType( resultType );
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

	@Deprecated(forRemoval = true)
	protected @Nullable ReturnableType<?> resolveResultType(
			Supplier<MappingModelExpressible<?>> inferredTypeSupplier,
			TypeConfiguration typeConfiguration) {
		if ( resultType == null ) {
			resultType = returnTypeResolver.resolveFunctionReturnType(
					impliedResultType,
					inferredTypeSupplier,
					getArguments(),
					typeConfiguration
			);
			setExpressibleType( resultType );
		}
		return resultType;
	}

	protected MappingModelExpressible<?> getMappingModelExpressible(
			SqmToSqlAstConverter walker,
			ReturnableType<?> resultType,
			List<SqlAstNode> arguments) {
		final MappingModelExpressible<?> mapping;
		if ( resultType instanceof MappingModelExpressible ) {
			// here we have a BasicType, which can be cast
			// directly to BasicValuedMapping
			mapping = (MappingModelExpressible<?>) resultType;
		}
		else {
			// here we have something that is not a BasicType,
			// and we have no way to get a BasicValuedMapping
			// from it directly
			mapping = returnTypeResolver.resolveFunctionReturnType(
					() -> {
						try {
							final MappingMetamodelImplementor domainModel = walker.getCreationContext()
									.getSessionFactory()
									.getRuntimeMetamodels()
									.getMappingMetamodel();
							return (BasicValuedMapping) domainModel.resolveMappingExpressible(
									getNodeType(),
									walker.getFromClauseAccess()::getTableGroup
							);
						}
						catch (Exception e) {
							return null; // this works at least approximately
						}
					},
					arguments
			);
		}
		return mapping;
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
			return argumentTypeResolver.resolveFunctionArgumentType( function, argumentIndex, converter );
		}
	}

}
