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
import org.hibernate.query.criteria.JpaJsonExistsExpression;
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
import org.hibernate.sql.ast.tree.expression.JsonExistsErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Special expression for the json_exists function that also captures special syntax elements like error behavior and passing variables.
 *
 * @since 7.0
 */
@Incubating
public class SqmJsonExistsExpression extends AbstractSqmJsonPathExpression<Boolean> implements JpaJsonExistsExpression {
	private ErrorBehavior errorBehavior = ErrorBehavior.UNSPECIFIED;

	public SqmJsonExistsExpression(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<Boolean> impliedResultType,
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

	private SqmJsonExistsExpression(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<Boolean> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name,
			@Nullable Map<String, SqmExpression<?>> passingExpressions,
			ErrorBehavior errorBehavior) {
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
	}

	public SqmJsonExistsExpression copy(SqmCopyContext context) {
		final SqmJsonExistsExpression existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( getArguments().size() );
		for ( SqmTypedNode<?> argument : getArguments() ) {
			arguments.add( argument.copy( context ) );
		}
		return context.registerCopy(
				this,
				new SqmJsonExistsExpression(
						getFunctionDescriptor(),
						getFunctionRenderer(),
						arguments,
						getImpliedResultType(),
						getArgumentsValidator(),
						getReturnTypeResolver(),
						nodeBuilder(),
						getFunctionName(),
						copyPassingExpressions( context ),
						errorBehavior
				)
		);
	}

	@Override
	public ErrorBehavior getErrorBehavior() {
		return errorBehavior;
	}

	@Override
	public SqmJsonExistsExpression unspecifiedOnError() {
		this.errorBehavior = ErrorBehavior.UNSPECIFIED;
		return this;
	}

	@Override
	public SqmJsonExistsExpression errorOnError() {
		this.errorBehavior = ErrorBehavior.ERROR;
		return this;
	}

	@Override
	public SqmJsonExistsExpression trueOnError() {
		this.errorBehavior = ErrorBehavior.TRUE;
		return this;
	}

	@Override
	public SqmJsonExistsExpression falseOnError() {
		this.errorBehavior = ErrorBehavior.FALSE;
		return this;
	}

	@Override
	public SqmJsonExistsExpression passing(
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
			case ERROR -> arguments.add( JsonExistsErrorBehavior.ERROR );
			case TRUE -> arguments.add( JsonExistsErrorBehavior.TRUE );
			case FALSE -> arguments.add( JsonExistsErrorBehavior.FALSE );
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
		hql.append( "json_exists(" );
		getArguments().get( 0 ).appendHqlString( hql, context );
		hql.append( ',' );
		getArguments().get( 1 ).appendHqlString( hql, context );

		appendPassingExpressionHqlString( hql, context );
		hql.append( switch ( errorBehavior ) {
			case ERROR -> " error on error";
			case TRUE -> " true on error";
			case FALSE -> " false on error";
			case UNSPECIFIED -> "";
		} );
		hql.append( ')' );
	}
}
