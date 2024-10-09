/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.derived.AnonymousTupleTableGroupProducer;
import org.hibernate.query.derived.AnonymousTupleType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
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

	public SelfRenderingSqmSetReturningFunction(
			SqmSetReturningFunctionDescriptor descriptor,
			SetReturningFunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver setReturningTypeResolver,
			AnonymousTupleType<T> type,
			NodeBuilder nodeBuilder,
			String name) {
		super( name, descriptor, type, arguments, nodeBuilder );
		this.renderer = renderer;
		this.argumentsValidator = argumentsValidator;
		this.setReturningTypeResolver = setReturningTypeResolver;
	}

	@Override
	public SelfRenderingSqmSetReturningFunction<T> copy(SqmCopyContext context) {
		final SelfRenderingSqmSetReturningFunction<T> existing = context.getCopy( this );
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
						getType(),
						nodeBuilder(),
						getFunctionName()
				)
		);
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
		if ( sqmArguments.isEmpty() ) {
			return emptyList();
		}
		final ArrayList<SqlAstNode> sqlAstArguments = new ArrayList<>( sqmArguments.size() );
		for ( int i = 0; i < sqmArguments.size(); i++ ) {
			sqlAstArguments.add(
					(SqlAstNode) sqmArguments.get( i ).accept( walker )
			);
		}
		return sqlAstArguments;
	}

	@Override
	public TableGroup convertToSqlAst(
			NavigablePath navigablePath,
			String identifierVariable,
			boolean lateral,
			boolean canUseInnerJoins,
			boolean withOrdinality,
			SqmToSqlAstConverter walker) {
		final List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
		final ArgumentsValidator validator = argumentsValidator;
		if ( validator != null ) {
			validator.validateSqlTypes( arguments, getFunctionName() );
		}
		final SelectableMapping[] selectableMappings = getSetReturningTypeResolver().resolveFunctionReturnType(
				arguments,
				identifierVariable,
				withOrdinality,
				walker.getCreationContext().getTypeConfiguration()
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
				getFunctionRenderer().rendersIdentifierVariable( arguments, walker.getCreationContext().getSessionFactory() ),
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
