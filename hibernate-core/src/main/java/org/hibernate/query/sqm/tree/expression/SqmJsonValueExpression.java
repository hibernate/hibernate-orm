/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaJsonValueExpression;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JsonValueEmptyBehavior;
import org.hibernate.sql.ast.tree.expression.JsonValueErrorBehavior;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Special expression for the json_value function that also captures special syntax elements like error and empty behavior.
 *
 * @since 7.0
 */
@Incubating
public class SqmJsonValueExpression<T> extends SelfRenderingSqmFunction<T> implements JpaJsonValueExpression<T> {
	private @Nullable ErrorBehavior errorBehavior;
	private SqmExpression<T> errorDefaultExpression;
	private @Nullable EmptyBehavior emptyBehavior;
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
			@Nullable ErrorBehavior errorBehavior,
			SqmExpression<T> errorDefaultExpression,
			@Nullable EmptyBehavior emptyBehavior,
			SqmExpression<T> emptyDefaultExpression) {
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
	public JpaJsonValueExpression<T> unspecifiedOnError() {
		this.errorBehavior = ErrorBehavior.UNSPECIFIED;
		this.errorDefaultExpression = null;
		return this;
	}

	@Override
	public JpaJsonValueExpression<T> errorOnError() {
		this.errorBehavior = ErrorBehavior.ERROR;
		this.errorDefaultExpression = null;
		return this;
	}

	@Override
	public JpaJsonValueExpression<T> nullOnError() {
		this.errorBehavior = ErrorBehavior.NULL;
		this.errorDefaultExpression = null;
		return this;
	}

	@Override
	public JpaJsonValueExpression<T> defaultOnError(jakarta.persistence.criteria.Expression<?> expression) {
		this.errorBehavior = ErrorBehavior.DEFAULT;
		//noinspection unchecked
		this.errorDefaultExpression = (SqmExpression<T>) expression;
		return this;
	}

	@Override
	public JpaJsonValueExpression<T> unspecifiedOnEmpty() {
		this.errorBehavior = ErrorBehavior.UNSPECIFIED;
		this.errorDefaultExpression = null;
		return this;
	}

	@Override
	public JpaJsonValueExpression<T> errorOnEmpty() {
		this.emptyBehavior = EmptyBehavior.ERROR;
		this.emptyDefaultExpression = null;
		return this;
	}

	@Override
	public JpaJsonValueExpression<T> nullOnEmpty() {
		this.emptyBehavior = EmptyBehavior.NULL;
		this.emptyDefaultExpression = null;
		return this;
	}

	@Override
	public JpaJsonValueExpression<T> defaultOnEmpty(jakarta.persistence.criteria.Expression<?> expression) {
		this.emptyBehavior = EmptyBehavior.DEFAULT;
		//noinspection unchecked
		this.emptyDefaultExpression = (SqmExpression<T>) expression;
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
		if ( errorBehavior != null ) {
			switch ( errorBehavior ) {
				case NULL:
					arguments.add( JsonValueErrorBehavior.NULL );
					break;
				case ERROR:
					arguments.add( JsonValueErrorBehavior.ERROR );
					break;
				case DEFAULT:
					arguments.add( JsonValueErrorBehavior.defaultOnError(
							(Expression) errorDefaultExpression.accept( walker )
					) );
					break;
			}
		}
		if ( emptyBehavior != null ) {
			switch ( emptyBehavior ) {
				case NULL:
					arguments.add( JsonValueEmptyBehavior.NULL );
					break;
				case ERROR:
					arguments.add( JsonValueEmptyBehavior.ERROR );
					break;
				case DEFAULT:
					arguments.add( JsonValueEmptyBehavior.defaultOnEmpty(
							(Expression) emptyDefaultExpression.accept( walker )
					) );
					break;
			}
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
	public void appendHqlString(StringBuilder sb) {
		sb.append( "json_value(" );
		getArguments().get( 0 ).appendHqlString( sb );
		sb.append( ',' );
		getArguments().get( 1 ).appendHqlString( sb );

		if ( getArguments().size() > 2 ) {
			sb.append( " returning " );
			getArguments().get( 2 ).appendHqlString( sb );
		}
		if ( errorBehavior != null ) {
			switch ( errorBehavior ) {
				case NULL:
					sb.append( " null on error" );
					break;
				case ERROR:
					sb.append( " error on error" );
					break;
				case DEFAULT:
					sb.append( " default " );
					errorDefaultExpression.appendHqlString( sb );
					sb.append( " on error" );
					break;
			}
		}
		if ( emptyBehavior != null ) {
			switch ( emptyBehavior ) {
				case NULL:
					sb.append( " null on empty" );
					break;
				case ERROR:
					sb.append( " error on empty" );
					break;
				case DEFAULT:
					sb.append( " default " );
					emptyDefaultExpression.appendHqlString( sb );
					sb.append( " on empty" );
					break;
			}
		}
		sb.append( ')' );
	}
}
