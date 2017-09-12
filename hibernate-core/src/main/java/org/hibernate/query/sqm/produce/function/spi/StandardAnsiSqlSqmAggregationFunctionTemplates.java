/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.SqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Centralized definition of standard ANSI SQL aggregation function templates
 *
 * @author Steve Ebersole
 */
public class StandardAnsiSqlSqmAggregationFunctionTemplates {
	/**
	 * Definition of a standard ANSI SQL compliant <tt>COUNT</tt> function
	 */
	public static class CountFunctionTemplate extends AbstractSqmFunctionTemplate {
		/**
		 * Singleton access
		 */
		public static final CountFunctionTemplate INSTANCE = new CountFunctionTemplate();

		private CountFunctionTemplate() {
			super( StandardArgumentsValidators.exactly( 1 ) );
		}

		@Override
		protected SqmExpression generateSqmFunctionExpression(
				List<SqmExpression> arguments,
				AllowableFunctionReturnType impliedResultType) {

			assert !arguments.isEmpty();
			if ( arguments.get( 0 ) == SqmCountStarFunction.STAR ) {
				return new SqmCountStarFunction( impliedResultType );
			}
			else {
				return new SqmCountFunction( arguments.get( 0 ), impliedResultType );
			}
		}
	}

	/**
	 * Definition of a standard ANSI SQL compliant <tt>AVG</tt> function
	 */
	public static class AvgFunctionTemplate extends AbstractSqmFunctionTemplate {
		/**
		 * Singleton access
		 */
		public static final AvgFunctionTemplate INSTANCE = new AvgFunctionTemplate( null );

		private final String sqlCastTypeForFloatingPointArgTypes;

		public AvgFunctionTemplate(String sqlCastTypeForFloatingPointArgTypes) {
			super( StandardArgumentsValidators.exactly( 1 ) );
			this.sqlCastTypeForFloatingPointArgTypes = sqlCastTypeForFloatingPointArgTypes;
		}

		@Override
		protected SqmExpression generateSqmFunctionExpression(
				List<SqmExpression> arguments,
				AllowableFunctionReturnType impliedResultType) {
			final SqmExpression argument = arguments.get( 0 );

			final Class argumentJavaType = argument.getExpressableType().getJavaType();
			final boolean isFloatingPointNumber = Float.class.isInstance( argumentJavaType )
					|| Double.class.isInstance( argumentJavaType );
			final boolean needsCast = sqlCastTypeForFloatingPointArgTypes != null && !isFloatingPointNumber;
			final SqmExpression argumentToPass = needsCast
					? cast( argument, sqlCastTypeForFloatingPointArgTypes )
					: argument;

			return new SqmAvgFunction( argumentToPass );
		}
	}

	/**
	 * Definition of a standard ANSI SQL compliant <tt>MAX</tt> function
	 */
	public static class MaxFunctionTemplate extends AbstractSqmFunctionTemplate {
		/**
		 * Singleton access
		 */
		public static final MaxFunctionTemplate INSTANCE = new MaxFunctionTemplate();

		protected MaxFunctionTemplate() {
			super( StandardArgumentsValidators.exactly( 1 ) );
		}

		@Override
		protected SqmExpression generateSqmFunctionExpression(
				List<SqmExpression> arguments,
				AllowableFunctionReturnType impliedResultType) {
			return new SqmMaxFunction( arguments.get( 0 ) );
		}
	}

	/**
	 * Definition of a standard ANSI SQL compliant <tt>MIN</tt> function
	 */
	public static class MinFunctionTemplate extends AbstractSqmFunctionTemplate {
		/**
		 * Singleton access
		 */
		public static final MinFunctionTemplate INSTANCE = new MinFunctionTemplate();

		protected MinFunctionTemplate() {
			super( StandardArgumentsValidators.exactly( 1 ) );
		}

		@Override
		protected SqmExpression generateSqmFunctionExpression(
				List<SqmExpression> arguments,
				AllowableFunctionReturnType impliedResultType) {
			return new SqmMinFunction( arguments.get( 0 ) );
		}
	}


	/**
	 * Definition of a standard ANSI SQL compliant <tt>SUM</tt> function
	 */
	public static class SumFunctionTemplate extends AbstractSqmFunctionTemplate {
		/**
		 * Singleton access
		 */
		public static final SumFunctionTemplate INSTANCE = new SumFunctionTemplate();

		protected SumFunctionTemplate() {
			super( StandardArgumentsValidators.exactly( 1 ) );
		}

		@Override
		protected SqmExpression generateSqmFunctionExpression(
				List<SqmExpression> arguments,
				AllowableFunctionReturnType impliedResultType) {
			final SqmExpression argument = arguments.get( 0 );
			return new SqmSumFunction(
					argument,
					deduceReturnType( argument.getExpressableType() )
			);
		}

		private AllowableFunctionReturnType deduceReturnType(ExpressableType argumentType) {
			final Class argumentJavaType = argumentType.getJavaType();

			// NOTE : this `isArgumentTypeAllowable` bit is an attempt to allow
			//		custom types associated with a Navigable to be used as-is..
			//		The idea being to incorporate AttributeConverters if applied
			final boolean isArgumentTypeAllowable = argumentType instanceof AllowableFunctionReturnType;
			if ( BigInteger.class.equals( argumentJavaType ) ) {
				return isArgumentTypeAllowable
						? (AllowableFunctionReturnType) argumentType
						: StandardSpiBasicTypes.BIG_INTEGER;
			}

			if ( BigDecimal.class.equals( argumentJavaType ) ) {
				return isArgumentTypeAllowable
						? (AllowableFunctionReturnType) argumentType
						: StandardSpiBasicTypes.BIG_DECIMAL;
			}

			if ( Long.class.equals( argumentJavaType ) ) {
				return isArgumentTypeAllowable
						? (AllowableFunctionReturnType) argumentType
						: StandardSpiBasicTypes.LONG;
			}

			if ( Double.class.equals( argumentJavaType ) ) {
				return isArgumentTypeAllowable
						? (AllowableFunctionReturnType) argumentType
						: StandardSpiBasicTypes.DOUBLE;
			}

			// if we get here we will not be able to use the incoming argument type...

			if ( Short.class.equals( argumentJavaType ) ) {
				return StandardSpiBasicTypes.LONG;
			}

			if ( Integer.class.equals( argumentJavaType ) ) {
				return StandardSpiBasicTypes.LONG;
			}

			if ( Float.class.equals( argumentJavaType ) ) {
				return StandardSpiBasicTypes.DOUBLE;
			}

			return StandardSpiBasicTypes.DOUBLE;
		}
	}

	/**
	 * Push the functions defined on StandardAnsiSqlAggregationFunctions into the given map
	 *
	 * @param functionMap The map of functions to push to
	 */
	public static void primeFunctionMap(Map<String, SqmFunctionTemplate> functionMap) {
		functionMap.put( SqmAvgFunction.NAME, AvgFunctionTemplate.INSTANCE );
		functionMap.put( SqmCountFunction.NAME, CountFunctionTemplate.INSTANCE );
		functionMap.put( SqmMaxFunction.NAME, MaxFunctionTemplate.INSTANCE );
		functionMap.put( SqmMinFunction.NAME, MinFunctionTemplate.INSTANCE );
		functionMap.put( SqmSumFunction.NAME, SumFunctionTemplate.INSTANCE );
	}

	private StandardAnsiSqlSqmAggregationFunctionTemplates() {
	}

	static SqmExpression cast(SqmExpression argument, String sqlCastTypeForFloatingPointArgTypes) {
		return new SqmCastFunction(
				argument,
				StandardSpiBasicTypes.DOUBLE,
				sqlCastTypeForFloatingPointArgTypes
		);
	}

}
