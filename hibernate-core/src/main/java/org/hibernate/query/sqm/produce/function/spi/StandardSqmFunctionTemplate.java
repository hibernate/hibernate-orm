/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;
import java.util.Locale;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

import org.jboss.logging.Logger;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public class StandardSqmFunctionTemplate extends AbstractSelfRenderingFunctionTemplate {
	private static final Logger log = Logger.getLogger( StandardSqmFunctionTemplate.class );

	private final String functionName;
	private final boolean useParenthesesWhenNoArgs;

	public StandardSqmFunctionTemplate(
			String functionName,
			boolean addParenthesisWhenNoArgs,
			AllowableFunctionReturnType invariantType,
			ArgumentsValidator argumentsValidator) {
		super( invariantType, argumentsValidator );

		this.functionName = functionName;
		this.useParenthesesWhenNoArgs = addParenthesisWhenNoArgs;
	}

	/**
	 * Construct a standard SQL function definition without a static return
	 * type - the actual return type will depend on the types of the arguments
	 * to which the function is applied.
	 * <p/>
	 * Using this form, the return type is considered variant and assumed
	 * to be the type of the *first argument*.
	 *
	 * @param functionName The name of the function.
	 */
	public StandardSqmFunctionTemplate(String functionName) {
		this( functionName, false );
	}

	public StandardSqmFunctionTemplate(String functionName, boolean addParenthesisWhenNoArgs) {
		this( functionName, addParenthesisWhenNoArgs, null, ArgumentsValidator.NONE );
	}

	public StandardSqmFunctionTemplate(
			String functionName,
			boolean addParenthesisWhenNoArgs,
			ArgumentsValidator argumentsValidator) {
		this( functionName, addParenthesisWhenNoArgs, null, argumentsValidator );
	}

	public StandardSqmFunctionTemplate(String functionName, ArgumentsValidator argumentsValidator) {
		this( functionName, true, null, argumentsValidator );
	}

	/**
	 * Construct a standard SQL function definition with an invariant return type.
	 *
	 * @param functionName The name of the function.
	 * @param invariantReturnType The invariable return type of the described
	 * 		function.  E.g. the SQL function `current_timestamp` *always* returns a timestamp -
	 * 		that is its invariant (unvarying) return type.
	 */
	public StandardSqmFunctionTemplate(String functionName, AllowableFunctionReturnType invariantReturnType) {
		this( functionName, true, invariantReturnType, ArgumentsValidator.NONE );
	}

	public StandardSqmFunctionTemplate(
			String functionName,
			AllowableFunctionReturnType invariantReturnType,
			ArgumentsValidator argumentsValidator) {
		this( functionName, true, invariantReturnType, argumentsValidator );
	}

	/**
	 * Function name accessor
	 *
	 * @return The function name.
	 */
	public String getName() {
		return functionName;
	}

	/**
	 * @deprecated Use {@link #getInvariantReturnType()} instead - more
	 * descriptive
	 */
	@Deprecated
	public AllowableFunctionReturnType getType() {
		return getInvariantReturnType();
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {
		return new RenderingSupport( arguments, impliedResultType );
	}

	private class RenderingSupport implements SelfRenderingFunctionSupport {
		final AllowableFunctionReturnType returnType;

		RenderingSupport(
				List<SqmExpression> arguments,
				AllowableFunctionReturnType impliedResultType) {
			this.returnType = resolveReturnType( arguments, impliedResultType );
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				List<Expression> sqlAstArguments,
				SqlAstWalker walker,
				SessionFactoryImplementor sessionFactory) {
			sqlAppender.appendSql( functionName );
			if ( useParenthesesWhenNoArgs || !sqlAstArguments.isEmpty() ) {
				sqlAppender.appendSql( "(" );
			}

			boolean firstPass = true;
			for ( Expression sqlAstArgument : sqlAstArguments ) {
				if ( !firstPass ) {
					sqlAppender.appendSql( ", " );
				}
				sqlAstArgument.accept( walker );
				firstPass = false;
			}

			if ( useParenthesesWhenNoArgs || !sqlAstArguments.isEmpty() ) {
				sqlAppender.appendSql( ")" );
			}
		}
	}

	private AllowableFunctionReturnType resolveReturnType(
			List<SqmExpression> arguments,
			AllowableFunctionReturnType impliedResultType) {

		AllowableFunctionReturnType dynamicType = impliedResultType;
		if ( !arguments.isEmpty() ) {
			final ExpressableType firstArgumentType = arguments.get( 0 ).getExpressionType();
			if ( firstArgumentType instanceof AllowableFunctionReturnType ) {
				dynamicType = (AllowableFunctionReturnType) firstArgumentType;
			}
		}

		if ( getInvariantReturnType() == null ) {
			if ( dynamicType == null ) {
				log.debugf(
						"Function template [%s] did not define invariant return type and a dynamic type could not be determined",
						this
				);
			}

			return dynamicType;
		}

		// we should still apply the implied result type or the first
		// 		argument type if they are "compatible" with the
		// 		invariantReturnType  because they may contain converters
		// 		or other site-specific information
		if ( dynamicType != null && areCompatible( getInvariantReturnType(), dynamicType) ) {
			return dynamicType;
		}

		return getInvariantReturnType();
	}

	private static boolean areCompatible(
			AllowableFunctionReturnType invariantReturnType,
			AllowableFunctionReturnType dynamicType) {
		assert invariantReturnType != null;
		assert dynamicType != null;

		// for now, just check the Java types
		return invariantReturnType.getJavaTypeDescriptor()
				.getJavaType()
				.equals(  dynamicType.getJavaTypeDescriptor().getJavaType() );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"StandardSqmFunctionTemplate(%s, %s)",
				functionName,
				getInvariantReturnType()
		);
	}

	public static class Builder {
		private final String name;

		private boolean useParenthesesWhenNoArgs;
		private AllowableFunctionReturnType invariantType;
		private ArgumentsValidator argumentsValidator;

		public Builder(String name) {
			this.name = name;
		}

		public Builder setUseParenthesesWhenNoArgs(boolean useParenthesesWhenNoArgs) {
			this.useParenthesesWhenNoArgs = useParenthesesWhenNoArgs;
			return this;
		}

		public Builder setInvariantType(AllowableFunctionReturnType invariantType) {
			this.invariantType = invariantType;
			return this;
		}

		public Builder setArgumentsValidator(ArgumentsValidator argumentsValidator) {
			this.argumentsValidator = argumentsValidator;
			return this;
		}

		public StandardSqmFunctionTemplate make() {
			return new StandardSqmFunctionTemplate(
					name,
					useParenthesesWhenNoArgs,
					invariantType,
					argumentsValidator
			);
		}
	}
}
