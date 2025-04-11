/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaJsonValueExpression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Special expression for the json_value function that also captures special syntax elements like error and empty behavior.
 *
 * @since 7.0
 */
@Incubating
public class SqmJsonValueExpression<T> extends AbstractSqmJsonPathExpression<T> implements JpaJsonValueExpression<T> {
	private ErrorBehavior errorBehavior = ErrorBehavior.UNSPECIFIED;
	private SqmExpression<T> errorDefaultExpression;
	private EmptyBehavior emptyBehavior = EmptyBehavior.UNSPECIFIED;
	private SqmExpression<T> emptyDefaultExpression;

	public SqmJsonValueExpression(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super(
				descriptor,
				renderer,
				arguments,
				impliedResultType,
				argumentsValidator,
				returnTypeResolver,
				nodeBuilder,
				name
		);
	}

	private SqmJsonValueExpression(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<T> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name,
			@Nullable Map<String, SqmExpression<?>> passingExpressions,
			ErrorBehavior errorBehavior,
			SqmExpression<T> errorDefaultExpression,
			EmptyBehavior emptyBehavior,
			SqmExpression<T> emptyDefaultExpression) {
		super(
				descriptor,
				renderer,
				arguments,
				impliedResultType,
				argumentsValidator,
				returnTypeResolver,
				nodeBuilder,
				name,
				passingExpressions
		);
		this.errorBehavior = errorBehavior;
		this.errorDefaultExpression = errorDefaultExpression;
		this.emptyBehavior = emptyBehavior;
		this.emptyDefaultExpression = emptyDefaultExpression;
	}

	public SqmJsonValueExpression<T> copy(SqmCopyContext context) {
		final SqmJsonValueExpression<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( getArguments().size() );
		for ( SqmTypedNode<?> argument : getArguments() ) {
			arguments.add( argument.copy( context ) );
		}
		return context.registerCopy(
				this,
				new SqmJsonValueExpression<>(
						getFunctionDescriptor(),
						getFunctionRenderer(),
						arguments,
						getImpliedResultType(),
						getArgumentsValidator(),
						getReturnTypeResolver(),
						nodeBuilder(),
						getFunctionName(),
						copyPassingExpressions( context ),
						errorBehavior,
						errorDefaultExpression == null ? null : errorDefaultExpression.copy( context ),
						emptyBehavior,
						emptyDefaultExpression == null ? null : emptyDefaultExpression.copy( context )
				)
		);
	}

	@Override
	public ErrorBehavior getErrorBehavior() {
		return errorBehavior;
	}

	@Override
	public EmptyBehavior getEmptyBehavior() {
		return emptyBehavior;
	}

	@Override
	public @Nullable JpaExpression<T> getErrorDefault() {
		return errorDefaultExpression;
	}

	@Override
	public @Nullable JpaExpression<T> getEmptyDefault() {
		return emptyDefaultExpression;
	}

	@Override
	public SqmJsonValueExpression<T> unspecifiedOnError() {
		this.errorBehavior = ErrorBehavior.UNSPECIFIED;
		this.errorDefaultExpression = null;
		return this;
	}

	@Override
	public SqmJsonValueExpression<T> errorOnError() {
		this.errorBehavior = ErrorBehavior.ERROR;
		this.errorDefaultExpression = null;
		return this;
	}

	@Override
	public SqmJsonValueExpression<T> nullOnError() {
		this.errorBehavior = ErrorBehavior.NULL;
		this.errorDefaultExpression = null;
		return this;
	}

	@Override
	public SqmJsonValueExpression<T> defaultOnError(jakarta.persistence.criteria.Expression<?> expression) {
		this.errorBehavior = ErrorBehavior.DEFAULT;
		//noinspection unchecked
		this.errorDefaultExpression = (SqmExpression<T>) expression;
		return this;
	}

	@Override
	public SqmJsonValueExpression<T> unspecifiedOnEmpty() {
		this.errorBehavior = ErrorBehavior.UNSPECIFIED;
		this.errorDefaultExpression = null;
		return this;
	}

	@Override
	public SqmJsonValueExpression<T> errorOnEmpty() {
		this.emptyBehavior = EmptyBehavior.ERROR;
		this.emptyDefaultExpression = null;
		return this;
	}

	@Override
	public SqmJsonValueExpression<T> nullOnEmpty() {
		this.emptyBehavior = EmptyBehavior.NULL;
		this.emptyDefaultExpression = null;
		return this;
	}

	@Override
	public SqmJsonValueExpression<T> defaultOnEmpty(jakarta.persistence.criteria.Expression<?> expression) {
		this.emptyBehavior = EmptyBehavior.DEFAULT;
		//noinspection unchecked
		this.emptyDefaultExpression = (SqmExpression<T>) expression;
		return this;
	}

	@Override
	public SqmJsonValueExpression<T> passing(
			String parameterName,
			jakarta.persistence.criteria.Expression<?> expression) {
		addPassingExpression( parameterName, (SqmExpression<?>) expression );
		return this;
	}

	@Override
	public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
		final @Nullable ReturnableType<?> resultType = resolveResultType( walker );
		final List<SqlAstNode> arguments = resolveSqlAstArguments( getArguments(), walker );
		final ArgumentsValidator validator = getArgumentsValidator();
		if ( validator != null ) {
			validator.validateSqlTypes( arguments, getFunctionName() );
		}
		final JsonPathPassingClause jsonPathPassingClause = createJsonPathPassingClause( walker );
		if ( jsonPathPassingClause != null ) {
			arguments.add( jsonPathPassingClause );
		}
		switch ( errorBehavior ) {
			case NULL -> arguments.add( JsonValueErrorBehavior.NULL );
			case ERROR -> arguments.add( JsonValueErrorBehavior.ERROR );
			case DEFAULT -> arguments.add( JsonValueErrorBehavior.defaultOnError(
					(Expression) errorDefaultExpression.accept( walker )
			) );
		}
		switch ( emptyBehavior ) {
			case NULL -> arguments.add( JsonValueEmptyBehavior.NULL );
			case ERROR -> arguments.add( JsonValueEmptyBehavior.ERROR );
			case DEFAULT -> arguments.add( JsonValueEmptyBehavior.defaultOnEmpty(
					(Expression) emptyDefaultExpression.accept( walker )
			) );
		}
		return new SelfRenderingFunctionSqlAstExpression(
				getFunctionName(),
				getFunctionRenderer(),
				arguments,
				resultType,
				resultType == null ? null : getMappingModelExpressible( walker, resultType, arguments )
		);
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "json_value(" );
		getArguments().get( 0 ).appendHqlString( hql, context );
		hql.append( ',' );
		getArguments().get( 1 ).appendHqlString( hql, context );

		appendPassingExpressionHqlString( hql, context );
		if ( getArguments().size() > 2 ) {
			hql.append( " returning " );
			getArguments().get( 2 ).appendHqlString( hql, context );
		}
		switch ( errorBehavior ) {
			case NULL -> hql.append( " null on error" );
			case ERROR -> hql.append( " error on error" );
			case DEFAULT -> {
				hql.append( " default " );
				errorDefaultExpression.appendHqlString( hql, context );
				hql.append( " on error" );
			}
		}
		switch ( emptyBehavior ) {
			case NULL -> hql.append( " null on empty" );
			case ERROR -> hql.append( " error on empty" );
			case DEFAULT -> {
				hql.append( " default " );
				emptyDefaultExpression.appendHqlString( hql, context );
				hql.append( " on empty" );
			}
		}
		hql.append( ')' );
	}
}
