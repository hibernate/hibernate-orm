/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.FunctionExpression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.emptyList;

/**
 * @since 7.0
 */
@Incubating
public class SelfRenderingSqmSetReturningFunction<T> extends SqmSetReturningFunction<T> {
	private final @Nullable ArgumentsValidator argumentsValidator;
	private final SetReturningFunctionTypeResolver setReturningTypeResolver;
	private final SetReturningFunctionRenderer renderer;
	private @Nullable AnonymousTupleType<T> type;

	public SelfRenderingSqmSetReturningFunction(
			SqmSetReturningFunctionDescriptor descriptor,
			SetReturningFunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver setReturningTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super( name, descriptor, arguments, nodeBuilder );
		this.renderer = renderer;
		this.argumentsValidator = argumentsValidator;
		this.setReturningTypeResolver = setReturningTypeResolver;
	}

	@Override
	public SelfRenderingSqmSetReturningFunction<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( getArguments().size() );
		for ( SqmTypedNode<?> argument : getArguments() ) {
			arguments.add( argument.copy( context ) );
		}
		return context.registerCopy(
				this,
				new SelfRenderingSqmSetReturningFunction<>(
						getFunctionDescriptor(),
						getFunctionRenderer(),
						arguments,
						getArgumentsValidator(),
						getSetReturningTypeResolver(),
						nodeBuilder(),
						getFunctionName()
				)
		);
	}

	@Override
	public AnonymousTupleType<T> getType() {
		AnonymousTupleType<T> type = this.type;
		if ( type == null ) {
			//noinspection unchecked
			type = this.type = (AnonymousTupleType<T>) getSetReturningTypeResolver().resolveTupleType(
					getArguments(),
					nodeBuilder().getTypeConfiguration()
			);
			if ( type == null ) {
				throw new IllegalStateException( "SetReturningFunctionTypeResolver returned a null tuple type" );
			}
		}
		return type;
	}

	protected boolean isTypeResolved() {
		return type != null;
	}

	public SetReturningFunctionRenderer getFunctionRenderer() {
		return renderer;
	}

	protected @Nullable ArgumentsValidator getArgumentsValidator() {
		return argumentsValidator;
	}

	public SetReturningFunctionTypeResolver getSetReturningTypeResolver() {
		return setReturningTypeResolver;
	}

	protected List<SqlAstNode> resolveSqlAstArguments(List<? extends SqmTypedNode<?>> sqmArguments, SqmToSqlAstConverter walker) {
		return resolveSqlAstArguments( sqmArguments, 0, sqmArguments.size(), walker );
	}

	protected List<SqlAstNode> resolveSqlAstArguments(List<? extends SqmTypedNode<?>> sqmArguments, int start, int end, SqmToSqlAstConverter walker) {
		if ( start == end ) {
			return emptyList();
		}
		final FunctionArgumentTypeResolver argumentTypeResolver =
				getFunctionDescriptor() instanceof AbstractSqmSetReturningFunctionDescriptor setReturningFunctionDescriptor
						? setReturningFunctionDescriptor.getArgumentTypeResolver()
						: null;
		final ArrayList<SqlAstNode> sqlAstArguments = new ArrayList<>( end - start );
		if ( argumentTypeResolver == null ) {
			for ( int i = start; i < end; i++ ) {
				sqlAstArguments.add( (SqlAstNode) sqmArguments.get( i ).accept( walker ) );
			}
		}
		else {
			final FunctionArgumentTypeResolverTypeAccess typeAccess = new FunctionArgumentTypeResolverTypeAccess(
					walker,
					this,
					argumentTypeResolver
			);
			for ( int i = start; i < end; i++ ) {
				typeAccess.argumentIndex = i;
				sqlAstArguments.add(
						(SqlAstNode) walker.visitWithInferredType( sqmArguments.get( i ), typeAccess )
				);
			}
		}
		return sqlAstArguments;
	}

	private static class FunctionArgumentTypeResolverTypeAccess implements Supplier<MappingModelExpressible<?>> {

		private final SqmToSqlAstConverter converter;
		private final SqmSetReturningFunction<?> function;
		private final FunctionArgumentTypeResolver argumentTypeResolver;
		private int argumentIndex;

		public FunctionArgumentTypeResolverTypeAccess(
				SqmToSqlAstConverter converter,
				SqmSetReturningFunction<?> function,
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
	public TableGroup convertToSqlAst(
			NavigablePath navigablePath,
			String identifierVariable,
			boolean lateral,
			boolean canUseInnerJoins,
			boolean withOrdinality,
			SqmToSqlAstConverter walker) {
		final List<SqlAstNode> arguments;
		try {
			arguments = resolveSqlAstArguments( getArguments(), walker );
		}
		catch ( SqlTreeCreationException ex ) {
			if ( !lateral && ex.getMessage().contains( "Could not locate TableGroup" ) ) {
				throw new IllegalArgumentException( "Could not construct set-returning function. Maybe you forgot to use 'lateral'?", ex );
			}
			throw ex;
		}
		final ArgumentsValidator validator = argumentsValidator;
		if ( validator != null ) {
			validator.validateSqlTypes( arguments, getFunctionName() );
		}
		final SelectableMapping[] selectableMappings = getSetReturningTypeResolver().resolveFunctionReturnType(
				arguments,
				identifierVariable,
				lateral,
				withOrdinality,
				walker
		);
		final AnonymousTupleTableGroupProducer tableGroupProducer = getType().resolveTableGroupProducer(
				identifierVariable,
				selectableMappings,
				walker.getFromClauseAccess()
		);
		return new FunctionTableGroup(
				navigablePath,
				tableGroupProducer,
				new SetReturningFunctionExpression(
						getFunctionName(),
						getFunctionRenderer(),
						arguments,
						tableGroupProducer,
						identifierVariable
				),
				identifierVariable,
				tableGroupProducer.getColumnNames(),
				tableGroupProducer.getCompatibleTableExpressions(),
				lateral,
				canUseInnerJoins,
				getFunctionRenderer()
						.rendersIdentifierVariable( arguments, walker.getCreationContext().getSessionFactory() ),
				walker.getCreationContext().getSessionFactory()
		);
	}

	private record SetReturningFunctionExpression(
			String functionName,
			SetReturningFunctionRenderer functionRenderer,
			List<SqlAstNode> arguments,
			AnonymousTupleTableGroupProducer tableGroupProducer,
			String tableIdentifierVariable
	) implements SelfRenderingExpression, FunctionExpression {

		@Override
		public String getFunctionName() {
			return functionName;
		}

		@Override
		public List<? extends SqlAstNode> getArguments() {
			return arguments;
		}

		@Override
		public void renderToSql(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> walker,
				SessionFactoryImplementor sessionFactory) {
			functionRenderer.render( sqlAppender, arguments, tableGroupProducer, tableIdentifierVariable, walker );
		}

		@Override
		public JdbcMappingContainer getExpressionType() {
			return tableGroupProducer;
		}
	}
}
