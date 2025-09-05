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
import org.hibernate.query.criteria.JpaJsonQueryExpression;
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
import org.hibernate.sql.ast.tree.expression.JsonQueryEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonQueryWrapMode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Special expression for the json_query function that also captures special syntax elements like error and empty behavior.
 *
 * @since 7.0
 */
@Incubating
public class SqmJsonQueryExpression extends AbstractSqmJsonPathExpression<String> implements JpaJsonQueryExpression {
	private WrapMode wrapMode = WrapMode.UNSPECIFIED;
	private ErrorBehavior errorBehavior = ErrorBehavior.UNSPECIFIED;
	private EmptyBehavior emptyBehavior = EmptyBehavior.UNSPECIFIED;

	public SqmJsonQueryExpression(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<String> impliedResultType,
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

	private SqmJsonQueryExpression(
			SqmFunctionDescriptor descriptor,
			FunctionRenderer renderer,
			List<? extends SqmTypedNode<?>> arguments,
			@Nullable ReturnableType<String> impliedResultType,
			@Nullable ArgumentsValidator argumentsValidator,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name,
			@Nullable Map<String, SqmExpression<?>> passingExpressions,
			WrapMode wrapMode,
			ErrorBehavior errorBehavior,
			EmptyBehavior emptyBehavior) {
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
		this.wrapMode = wrapMode;
		this.errorBehavior = errorBehavior;
		this.emptyBehavior = emptyBehavior;
	}

	public SqmJsonQueryExpression copy(SqmCopyContext context) {
		final SqmJsonQueryExpression existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmTypedNode<?>> arguments = new ArrayList<>( getArguments().size() );
		for ( SqmTypedNode<?> argument : getArguments() ) {
			arguments.add( argument.copy( context ) );
		}
		return context.registerCopy(
				this,
				new SqmJsonQueryExpression(
						getFunctionDescriptor(),
						getFunctionRenderer(),
						arguments,
						getImpliedResultType(),
						getArgumentsValidator(),
						getReturnTypeResolver(),
						nodeBuilder(),
						getFunctionName(),
						copyPassingExpressions( context ),
						wrapMode,
						errorBehavior,
						emptyBehavior
				)
		);
	}

	@Override
	public WrapMode getWrapMode() {
		return wrapMode;
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
	public SqmJsonQueryExpression withoutWrapper() {
		this.wrapMode = WrapMode.WITHOUT_WRAPPER;
		return this;
	}

	@Override
	public SqmJsonQueryExpression withWrapper() {
		this.wrapMode = WrapMode.WITH_WRAPPER;
		return this;
	}

	@Override
	public SqmJsonQueryExpression withConditionalWrapper() {
		this.wrapMode = WrapMode.WITH_CONDITIONAL_WRAPPER;
		return this;
	}

	@Override
	public SqmJsonQueryExpression unspecifiedWrapper() {
		this.wrapMode = WrapMode.UNSPECIFIED;
		return this;
	}

	@Override
	public SqmJsonQueryExpression unspecifiedOnError() {
		this.errorBehavior = ErrorBehavior.UNSPECIFIED;
		return this;
	}

	@Override
	public SqmJsonQueryExpression errorOnError() {
		this.errorBehavior = ErrorBehavior.ERROR;
		return this;
	}

	@Override
	public SqmJsonQueryExpression nullOnError() {
		this.errorBehavior = ErrorBehavior.NULL;
		return this;
	}

	@Override
	public SqmJsonQueryExpression emptyArrayOnError() {
		this.errorBehavior = ErrorBehavior.EMPTY_ARRAY;
		return this;
	}

	@Override
	public SqmJsonQueryExpression emptyObjectOnError() {
		this.errorBehavior = ErrorBehavior.EMPTY_OBJECT;
		return this;
	}

	@Override
	public SqmJsonQueryExpression unspecifiedOnEmpty() {
		this.errorBehavior = ErrorBehavior.UNSPECIFIED;
		return this;
	}

	@Override
	public SqmJsonQueryExpression errorOnEmpty() {
		this.emptyBehavior = EmptyBehavior.ERROR;
		return this;
	}

	@Override
	public SqmJsonQueryExpression nullOnEmpty() {
		this.emptyBehavior = EmptyBehavior.NULL;
		return this;
	}

	@Override
	public SqmJsonQueryExpression emptyArrayOnEmpty() {
		this.emptyBehavior = EmptyBehavior.EMPTY_ARRAY;
		return this;
	}

	@Override
	public SqmJsonQueryExpression emptyObjectOnEmpty() {
		this.emptyBehavior = EmptyBehavior.EMPTY_OBJECT;
		return this;
	}

	@Override
	public SqmJsonQueryExpression passing(
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
		switch ( wrapMode ) {
			case WITH_WRAPPER -> arguments.add( JsonQueryWrapMode.WITH_WRAPPER );
			case WITHOUT_WRAPPER -> arguments.add( JsonQueryWrapMode.WITHOUT_WRAPPER );
			case WITH_CONDITIONAL_WRAPPER -> arguments.add( JsonQueryWrapMode.WITH_CONDITIONAL_WRAPPER );
		}
		switch ( errorBehavior ) {
			case NULL -> arguments.add( JsonQueryErrorBehavior.NULL );
			case ERROR -> arguments.add( JsonQueryErrorBehavior.ERROR );
			case EMPTY_OBJECT -> arguments.add( JsonQueryErrorBehavior.EMPTY_OBJECT );
			case EMPTY_ARRAY -> arguments.add( JsonQueryErrorBehavior.EMPTY_ARRAY );
		}
		switch ( emptyBehavior ) {
			case NULL -> arguments.add( JsonQueryEmptyBehavior.NULL );
			case ERROR -> arguments.add( JsonQueryEmptyBehavior.ERROR );
			case EMPTY_OBJECT -> arguments.add( JsonQueryEmptyBehavior.EMPTY_OBJECT );
			case EMPTY_ARRAY -> arguments.add( JsonQueryEmptyBehavior.EMPTY_ARRAY );
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
		hql.append( "json_query(" );
		getArguments().get( 0 ).appendHqlString( hql, context );
		hql.append( ',' );
		getArguments().get( 1 ).appendHqlString( hql, context );

		appendPassingExpressionHqlString( hql, context );
		switch ( wrapMode ) {
			case WITH_WRAPPER -> hql.append( " with wrapper" );
			case WITHOUT_WRAPPER -> hql.append( " without wrapper" );
			case WITH_CONDITIONAL_WRAPPER -> hql.append( " with conditional wrapper" );
		}
		switch ( errorBehavior ) {
			case NULL -> hql.append( " null on error" );
			case ERROR -> hql.append( " error on error" );
			case EMPTY_ARRAY -> hql.append( " empty array on error" );
			case EMPTY_OBJECT -> hql.append( " empty object on error" );
		}
		switch ( emptyBehavior ) {
			case NULL -> hql.append( " null on empty" );
			case ERROR -> hql.append( " error on empty" );
			case EMPTY_ARRAY -> hql.append( " empty array on empty" );
			case EMPTY_OBJECT -> hql.append( " empty object on empty" );
		}
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object other) {
		return super.equals( other )
			&& other instanceof SqmJsonQueryExpression that
			&& wrapMode == that.wrapMode
			&& errorBehavior == that.errorBehavior
			&& emptyBehavior == that.emptyBehavior;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + wrapMode.hashCode();
		result = 31 * result + errorBehavior.hashCode();
		result = 31 * result + emptyBehavior.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object other) {
		return super.isCompatible( other )
			&& other instanceof SqmJsonQueryExpression that
			&& wrapMode == that.wrapMode
			&& errorBehavior == that.errorBehavior
			&& emptyBehavior == that.emptyBehavior;
	}

	@Override
	public int cacheHashCode() {
		int result = super.cacheHashCode();
		result = 31 * result + wrapMode.hashCode();
		result = 31 * result + errorBehavior.hashCode();
		result = 31 * result + emptyBehavior.hashCode();
		return result;
	}
}
