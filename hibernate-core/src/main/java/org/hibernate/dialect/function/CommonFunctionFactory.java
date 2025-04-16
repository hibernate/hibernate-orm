/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.Date;
import java.util.Arrays;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.Dialect;

import org.hibernate.dialect.function.array.*;
import org.hibernate.dialect.function.json.*;
import org.hibernate.dialect.function.xml.DB2XmlTableFunction;
import org.hibernate.dialect.function.xml.GaussDBXmlQueryFunction;
import org.hibernate.dialect.function.xml.H2XmlConcatFunction;
import org.hibernate.dialect.function.xml.H2XmlElementFunction;
import org.hibernate.dialect.function.xml.H2XmlForestFunction;
import org.hibernate.dialect.function.xml.H2XmlPiFunction;
import org.hibernate.dialect.function.xml.HANAXmlTableFunction;
import org.hibernate.dialect.function.xml.LegacyDB2XmlExistsFunction;
import org.hibernate.dialect.function.xml.LegacyDB2XmlQueryFunction;
import org.hibernate.dialect.function.xml.OracleXmlTableFunction;
import org.hibernate.dialect.function.xml.PostgreSQLXmlQueryFunction;
import org.hibernate.dialect.function.xml.SQLServerXmlAggFunction;
import org.hibernate.dialect.function.xml.SQLServerXmlConcatFunction;
import org.hibernate.dialect.function.xml.SQLServerXmlElementFunction;
import org.hibernate.dialect.function.xml.SQLServerXmlExistsFunction;
import org.hibernate.dialect.function.xml.SQLServerXmlForestFunction;
import org.hibernate.dialect.function.xml.SQLServerXmlPiFunction;
import org.hibernate.dialect.function.xml.SQLServerXmlQueryFunction;
import org.hibernate.dialect.function.xml.SQLServerXmlTableFunction;
import org.hibernate.dialect.function.xml.SybaseASEXmlTableFunction;
import org.hibernate.dialect.function.xml.XmlAggFunction;
import org.hibernate.dialect.function.xml.XmlConcatFunction;
import org.hibernate.dialect.function.xml.XmlElementFunction;
import org.hibernate.dialect.function.xml.XmlExistsFunction;
import org.hibernate.dialect.function.xml.XmlForestFunction;
import org.hibernate.dialect.function.xml.XmlPiFunction;
import org.hibernate.dialect.function.xml.XmlQueryFunction;
import org.hibernate.dialect.function.xml.XmlTableFunction;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.*;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * Enumeratoes common function template definitions.
 * Centralized for easier use from dialects.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class CommonFunctionFactory {

	private final BasicType<Boolean> booleanType;
	private final BasicType<Character> characterType;
	private final BasicType<String> stringType;
	private final BasicType<byte[]> binaryType;
	private final BasicType<Integer> integerType;
	private final BasicType<Long> longType;
	private final BasicType<Double> doubleType;
	private final BasicType<Date> dateType;
	private final BasicType<Date> timeType;
	private final BasicType<Date> timestampType;

	private final SqmFunctionRegistry functionRegistry;
	private final TypeConfiguration typeConfiguration;

	public CommonFunctionFactory(FunctionContributions functionContributions) {
		functionRegistry = functionContributions.getFunctionRegistry();
		typeConfiguration = functionContributions.getTypeConfiguration();

		BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		dateType = basicTypeRegistry.resolve(StandardBasicTypes.DATE);
		timeType = basicTypeRegistry.resolve(StandardBasicTypes.TIME);
		timestampType = basicTypeRegistry.resolve(StandardBasicTypes.TIMESTAMP);
		longType = basicTypeRegistry.resolve(StandardBasicTypes.LONG);
		characterType = basicTypeRegistry.resolve(StandardBasicTypes.CHARACTER);
		booleanType = basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN);
		stringType = basicTypeRegistry.resolve(StandardBasicTypes.STRING);
		binaryType = basicTypeRegistry.resolve(StandardBasicTypes.BINARY);
		integerType = basicTypeRegistry.resolve(StandardBasicTypes.INTEGER);
		doubleType = basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// trigonometric/geometric functions

	public void cot() {
		functionRegistry.namedDescriptorBuilder( "cot" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	/**
	 * For databases where the first parameter is the base
	 */
	public void log() {
		functionRegistry.namedDescriptorBuilder( "log" )
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void log_ln() {
		functionRegistry.patternDescriptorBuilder( "log", "ln(?2)/ln(?1)" )
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.setInvariantType(doubleType)
				.setArgumentListSignature("(NUMERIC base, NUMERIC arg)")
				.register();
	}

	/**
	 * SQL Server defines parameters in reverse order
	 */
	public void log_log() {
		functionRegistry.patternDescriptorBuilder( "log", "log(?2,?1)" )
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.setInvariantType(doubleType)
				.setArgumentListSignature("(NUMERIC base, NUMERIC arg)")
				.register();
	}

	/**
	 * For Sybase
	 */
	public void log_loglog() {
		functionRegistry.patternDescriptorBuilder( "log", "log(?2)/log(?1)" )
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.setInvariantType(doubleType)
				.setArgumentListSignature("(NUMERIC base, NUMERIC arg)")
				.register();
	}

	/**
	 * For SQL Server and Sybase
	 */
	public void ln_log() {
		functionRegistry.namedDescriptorBuilder( "ln", "log" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void log10() {
		functionRegistry.namedDescriptorBuilder( "log10" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	/**
	 * For Oracle and HANA
	 */
	public void log10_log() {
		functionRegistry.patternDescriptorBuilder( "log10", "log(10,?1)" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void log2() {
		functionRegistry.namedDescriptorBuilder( "log2" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void radians() {
		functionRegistry.namedDescriptorBuilder( "radians" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	/**
	 * For Oracle, HANA
	 */
	public void radians_acos() {
		functionRegistry.patternDescriptorBuilder( "radians", "(?1*acos(-1)/180)" )
				.setInvariantType(doubleType)
				.setExactArgumentCount(1)
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void degrees() {
		functionRegistry.namedDescriptorBuilder( "degrees" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	/**
	 * For Oracle, HANA
	 */
	public void degrees_acos() {
		functionRegistry.patternDescriptorBuilder( "degrees", "(?1/acos(-1)*180)" )
				.setInvariantType(doubleType)
				.setExactArgumentCount(1)
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void sinh() {
		functionRegistry.namedDescriptorBuilder( "sinh" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void sinh_exp() {
		functionRegistry.patternDescriptorBuilder( "sinh", "((exp(?1)-exp(-?1))/2)" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void cosh() {
		functionRegistry.namedDescriptorBuilder( "cosh" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void cosh_exp() {
		functionRegistry.patternDescriptorBuilder( "cosh", "((exp(?1)+exp(-?1))/2)" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void tanh() {
		functionRegistry.namedDescriptorBuilder( "tanh" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void tanh_exp() {
		functionRegistry.patternDescriptorBuilder( "tanh", "((exp(2*?1)-1)/(exp(2*?1)+1))" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.setInvariantType(doubleType)
				.register();
	}

	public void moreHyperbolic() {
		functionRegistry.namedDescriptorBuilder( "acosh" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedDescriptorBuilder( "asinh" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedDescriptorBuilder( "atanh" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// numeric and datetime truncation

	private void trunc(
			String truncPattern,
			String twoArgTruncPattern,
			TruncFunction.DatetimeTrunc datetimeTrunc,
			String toDateFunction) {
		functionRegistry.register(
				"trunc",
				new TruncFunction( truncPattern, twoArgTruncPattern, datetimeTrunc, toDateFunction, typeConfiguration )
		);
		functionRegistry.registerAlternateKey( "truncate", "trunc" );
	}

	private void trunc(TruncFunction.DatetimeTrunc datetimeTrunc) {
		trunc( "trunc(?1)", "trunc(?1,?2)", datetimeTrunc, null );
	}

	public void trunc() {
		trunc( null );
	}

	public void trunc_dateTrunc() {
		trunc( TruncFunction.DatetimeTrunc.DATE_TRUNC );
	}

	public void trunc_dateTrunc_trunc() {
		trunc( TruncFunction.DatetimeTrunc.TRUNC );
	}

	/**
	 * MySQL
	 */
	public void trunc_truncate() {
		trunc( "truncate(?1,0)", "truncate(?1,?2)", TruncFunction.DatetimeTrunc.FORMAT, "str_to_date" );
	}

	/**
	 * SQL Server >= 16
	 */
	public void trunc_round_datetrunc() {
		trunc( "round(?1,0,1)", "round(?1,?2,1)", TruncFunction.DatetimeTrunc.DATETRUNC, "convert" );
	}

	/**
	 * Derby (only works if the second arg is constant, as it almost always is)
	 */
	public void trunc_floor() {
		trunc( "sign(?1)*floor(abs(?1))", "sign(?1)*floor(abs(?1)*1e?2)/1e?2", null, null );
	}

	/**
	 * SAP HANA
	 */
	public void trunc_roundMode() {
		trunc( "round(?1,0,round_down)", "round(?1,?2,round_down)", TruncFunction.DatetimeTrunc.FORMAT, "to_timestamp" );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basic math functions

	/**
	 * Returns double between 0.0 and 1.0. First call may specify a seed value.
	 */
	public void rand() {
		functionRegistry.namedDescriptorBuilder( "rand" )
				.setArgumentCountBetween( 0, 1 )
				.setParameterTypes(INTEGER)
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType(doubleType)
				.setArgumentListSignature( "([INTEGER seed])" )
				.register();
	}

	public void median() {
		functionRegistry.namedAggregateDescriptorBuilder( "median" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void median_percentileCont(boolean over) {
		functionRegistry.patternDescriptorBuilder(
						"median",
						"percentile_cont(0.5) within group (order by ?1)"
								+ ( over ? " over()" : "" )
				)
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	/**
	 * CockroachDB lacks
	 * <a href="https://github.com/cockroachdb/cockroach/issues/89965">implicit casting</a>
	 */
	public void median_percentileCont_castDouble() {
		functionRegistry.patternDescriptorBuilder(
						"median",
						"percentile_cont(0.5) within group (order by cast(?1 as double precision))"
				)
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	/**
	 * Warning: the semantics of this function are inconsistent between DBs.
	 * <ul>
	 * <li>On Postgres it means {@code stdev_samp()}
	 * <li>On Oracle, DB2, MySQL it means {@code  stdev_pop()}
	 * </ul>
	 */
	public void stddev() {
		functionRegistry.namedAggregateDescriptorBuilder( "stddev" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	/**
	 * Warning: the semantics of this function are inconsistent between DBs.
	 * <ul>
	 * <li>On Postgres it means {@code var_samp()}
	 * <li>On Oracle, DB2, MySQL it means {@code var_pop()}
	 * </ul>
	 */
	public void variance() {
		functionRegistry.namedAggregateDescriptorBuilder( "variance" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void stddevPopSamp() {
		functionRegistry.namedAggregateDescriptorBuilder( "stddev_pop" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "stddev_samp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void varPopSamp() {
		functionRegistry.namedAggregateDescriptorBuilder( "var_pop" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "var_samp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void covarPopSamp() {
		functionRegistry.namedAggregateDescriptorBuilder( "covar_pop" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "covar_samp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void corr() {
		functionRegistry.namedAggregateDescriptorBuilder( "corr" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void regrLinearRegressionAggregates() {
		Arrays.asList(
						"regr_avgx", "regr_avgy", "regr_count", "regr_intercept", "regr_r2",
						"regr_slope", "regr_sxx", "regr_sxy", "regr_syy"
				)
				.forEach(
						fnName -> functionRegistry.namedAggregateDescriptorBuilder( fnName )
								.setInvariantType(doubleType)
								.setExactArgumentCount( 2 )
								.setParameterTypes(NUMERIC, NUMERIC)
								.register()
				);
	}

	/**
	 * DB2
	 */
	public void varianceSamp() {
		functionRegistry.namedAggregateDescriptorBuilder( "variance_samp" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.setParameterTypes( NUMERIC )
				.register();
	}

	private static final String VAR_SAMP_SUM_COUNT_PATTERN = "(sum(power(?1,2))-(power(sum(?1),2)/count(?1)))/nullif(count(?1)-1,0)";

	/**
	 * DB2 before 11
	 */
	public void varSamp_sumCount() {
		functionRegistry.patternAggregateDescriptorBuilder( "var_samp", VAR_SAMP_SUM_COUNT_PATTERN )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.setParameterTypes( NUMERIC )
				.register();
	}

	/**
	 * DB2 before 11
	 */
	public void stddevSamp_sumCount() {
		functionRegistry.patternAggregateDescriptorBuilder( "stddev_samp", "sqrt(" + VAR_SAMP_SUM_COUNT_PATTERN + ")" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.setParameterTypes( NUMERIC )
				.register();
	}

	/**
	 * SQL Server-style
	 */
	public void stddevPopSamp_stdevp() {
		functionRegistry.namedAggregateDescriptorBuilder( "stdev" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "stdevp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.registerAlternateKey( "stddev_samp", "stdev" );
		functionRegistry.registerAlternateKey( "stddev_pop", "stdevp" );
	}

	/**
	 * SQL Server-style
	 */
	public void varPopSamp_varp() {
		functionRegistry.namedAggregateDescriptorBuilder( "var" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "varp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
		functionRegistry.registerAlternateKey( "var_samp", "var" );
		functionRegistry.registerAlternateKey( "var_pop", "varp" );
	}

	public void pi() {
		functionRegistry.noArgsBuilder( "pi" )
				.setInvariantType(doubleType)
				.setUseParenthesesWhenNoArgs( true )
				.setArgumentListSignature("")
				.register();
	}

	public void pi_acos() {
		functionRegistry.patternDescriptorBuilder( "pi", "acos(-1)" )
				.setInvariantType(doubleType)
				.setExactArgumentCount(0)
				.setArgumentListSignature("")
				.register();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// character functions

	public void soundex() {
		functionRegistry.namedDescriptorBuilder( "soundex" )
				.setExactArgumentCount( 1 )
				.setInvariantType(stringType)
				.register();
	}

	public void trim2() {
		functionRegistry.namedDescriptorBuilder( "ltrim" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(STRING, STRING)
				.setArgumentListSignature( "(STRING string[, STRING characters])" )
				.register();
		functionRegistry.namedDescriptorBuilder( "rtrim" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(STRING, STRING)
				.setArgumentListSignature( "(STRING string[, STRING characters])" )
				.register();
	}

	public void trim1() {
		functionRegistry.namedDescriptorBuilder( "ltrim" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "rtrim" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string)" )
				.register();
	}

	public void pad() {
		functionRegistry.namedDescriptorBuilder( "lpad" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, STRING)
				.setArgumentListSignature( "(STRING string, INTEGER length[, STRING padding])" )
				.register();
		functionRegistry.namedDescriptorBuilder( "rpad" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, STRING)
				.setArgumentListSignature( "(STRING string, INTEGER length[, STRING padding])" )
				.register();
	}

	/**
	 * In MySQL the third argument is required
	 */
	public void pad_space() {
		functionRegistry.registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"lpad(?1,?2,' ')",
				"lpad(?1,?2,?3)",
				STRING, INTEGER, STRING,
				typeConfiguration
		).setArgumentListSignature( "(string, length[, padding])" );
		functionRegistry.registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"rpad(?1,?2,' ')",
				"rpad(?1,?2,?3)",
				STRING, INTEGER, STRING,
				typeConfiguration
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	/**
	 * Transact-SQL
	 */
	public void pad_replicate() {
		functionRegistry.registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"(space(?2-len(?1))+?1)",
				"(replicate(?3,?2-len(?1))+?1)",
				STRING, INTEGER, STRING,
				typeConfiguration
		).setArgumentListSignature( "(string, length[, padding])" );
		functionRegistry.registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"(?1+space(?2-len(?1)))",
				"(?1+replicate(?3,?2-len(?1)))",
				STRING, INTEGER, STRING,
				typeConfiguration
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	public void pad_repeat() {
		functionRegistry.registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"(repeat(' ',?2-character_length(?1))||?1)",
				"(repeat(?3,?2-character_length(?1))||?1)",
				STRING, INTEGER, STRING,
				typeConfiguration
		).setArgumentListSignature( "(string, length[, padding])" );
		functionRegistry.registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"(?1||repeat(' ',?2-character_length(?1)))",
				"(?1||repeat(?3,?2-character_length(?1)))",
				STRING, INTEGER, STRING,
				typeConfiguration
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	/**
	 * SAP DB
	 */
	public void pad_fill() {
		functionRegistry.registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"lfill(?1,' ',?2)",
				"lfill(?1,?3,?2)",
				STRING, INTEGER, STRING,
				typeConfiguration
		).setArgumentListSignature( "(string, length[, padding])" );
		functionRegistry.registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"rfill(?1,' ',?2)",
				"rfill(?1,?3,?2)",
				STRING, INTEGER, STRING,
				typeConfiguration
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	public void reverse() {
		functionRegistry.namedDescriptorBuilder( "reverse" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.register();
	}

	public void space() {
		functionRegistry.namedDescriptorBuilder( "space" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(INTEGER)
				.register();
	}

	public void repeat() {
		functionRegistry.namedDescriptorBuilder( "repeat" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER times)" )
				.register();
	}

	public void repeat_rpad() {
		functionRegistry.patternDescriptorBuilder( "repeat", "rpad(?1,?2*length(?1),?1)" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER times)" )
				.register();
	}

	public void leftRight() {
		functionRegistry.namedDescriptorBuilder( "left" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "right" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
	}

	public void leftRight_substr() {
		functionRegistry.patternDescriptorBuilder( "left", "substr(?1,1,?2)" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
		functionRegistry.patternDescriptorBuilder( "right", "substr(?1,-?2)" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
	}

	/**
	 * Emulate left via substr and right via substr and length.
	 * This function is for Apache Derby and uses {@link SqlAstNodeRenderingMode#NO_PLAIN_PARAMETER}
	 * for the right function emulation, because length in Apache Derby can't handle plain parameters.
	 */
	public void leftRight_substrLength() {
		functionRegistry.patternDescriptorBuilder( "left", "substr(?1,1,?2)" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.register();
		functionRegistry.patternDescriptorBuilder( "right", "substr(?1,length(?1)-?2+1)" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER length)" )
				.setArgumentRenderingMode( SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER )
				.register();
	}

	public void repeat_replicate() {
		functionRegistry.namedDescriptorBuilder( "replicate" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER times)" )
				.register();
		functionRegistry.registerAlternateKey( "repeat", "replicate" );
	}

	@Deprecated(since = "7")
	public void md5() {
		functionRegistry.namedDescriptorBuilder( "md5" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.register();
	}

	public void initcap() {
		functionRegistry.namedDescriptorBuilder( "initcap" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.register();
	}

	public void instr() {
		functionRegistry.namedDescriptorBuilder( "instr" )
				.setInvariantType(integerType)
				.setArgumentCountBetween( 2, 4 )
				.setParameterTypes(STRING, STRING, INTEGER, INTEGER)
				.setArgumentListSignature( "(STRING string, STRING pattern[, INTEGER start[, INTEGER occurrence]])" )
				.register();
	}

	public void substr() {
		functionRegistry.namedDescriptorBuilder( "substr" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER start[, INTEGER length])" )
				.register();
	}

	public void translate() {
		functionRegistry.namedDescriptorBuilder( "translate" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 3 )
				.setParameterTypes( STRING, STRING, STRING )
				.register();
	}

	public void bitand() {
		functionRegistry.namedDescriptorBuilder( "bitand" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
	}

	public void bitor() {
		functionRegistry.namedDescriptorBuilder( "bitor" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
	}

	public void bitxor() {
		functionRegistry.namedDescriptorBuilder( "bitxor" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
	}

	public void bitnot() {
		functionRegistry.namedDescriptorBuilder( "bitnot" )
				.setExactArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.register();
	}

	/**
	 * Binary bitwise operators, not aggregate functions!
	 */
	public void bitandorxornot_bitAndOrXorNot() {
		functionRegistry.namedDescriptorBuilder( "bit_and" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.registerAlternateKey( "bitand", "bit_and" );

		functionRegistry.namedDescriptorBuilder( "bit_or" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.registerAlternateKey( "bitor", "bit_or" );

		functionRegistry.namedDescriptorBuilder( "bit_xor" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.registerAlternateKey( "bitxor", "bit_xor" );

		functionRegistry.namedDescriptorBuilder( "bit_not" )
				.setExactArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.registerAlternateKey( "bitnot", "bit_not" );
	}

	/**
	 * Bitwise operators, not aggregate functions!
	 */
	public void bitandorxornot_binAndOrXorNot() {
		functionRegistry.namedDescriptorBuilder( "bin_and" )
				.setMinArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.registerAlternateKey( "bitand", "bin_and" );

		functionRegistry.namedDescriptorBuilder( "bin_or" )
				.setMinArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.registerAlternateKey( "bitor", "bin_or" );

		functionRegistry.namedDescriptorBuilder( "bin_xor" )
				.setMinArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.registerAlternateKey( "bitxor", "bin_xor" );

		functionRegistry.namedDescriptorBuilder( "bin_not" )
				.setExactArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.registerAlternateKey( "bitnot", "bin_not" );
	}

	/**
	 * Binary bitwise operators, not aggregate functions!
	 */
	public void bitandorxornot_operator() {
		functionRegistry.patternDescriptorBuilder( "bitand", "(?1&?2)" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionRegistry.patternDescriptorBuilder( "bitor", "(?1|?2)" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionRegistry.patternDescriptorBuilder( "bitxor", "(?1^?2)" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionRegistry.patternDescriptorBuilder( "bitnot", "~?1" )
				.setExactArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.register();
	}

	/**
	 * These are aggregate functions taking one argument!
	 */
	public void bitAndOr() {
		functionRegistry.namedAggregateDescriptorBuilder( "bit_and" )
				.setExactArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		functionRegistry.namedAggregateDescriptorBuilder( "bit_or" )
				.setExactArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		//MySQL has it but how is that even useful?
//		functionRegistry.namedTemplateBuilder( "bit_xor" )
//				.setExactArgumentCount( 1 )
//				.register();
	}

	/**
	 * These are aggregate functions taking one argument!
	 */
	public void everyAny() {
		functionRegistry.namedAggregateDescriptorBuilder( "every" )
				.setExactArgumentCount( 1 )
				.setInvariantType(booleanType)
				.setParameterTypes(BOOLEAN)
				.setArgumentListSignature( "(BOOLEAN predicate)" )
				.register();

		functionRegistry.namedAggregateDescriptorBuilder( "any" )
				.setExactArgumentCount( 1 )
				.setInvariantType(booleanType)
				.setParameterTypes(BOOLEAN)
				.setArgumentListSignature( "(BOOLEAN predicate)" )
				.register();
	}

	/**
	 * These are aggregate functions taking one argument, for
	 * databases that can directly aggregate both boolean columns
	 * and predicates!
	 */
	public void everyAny_boolAndOr() {
		functionRegistry.namedAggregateDescriptorBuilder( "bool_and" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(BOOLEAN)
				.setInvariantType(booleanType)
				.setArgumentListSignature( "(BOOLEAN predicate)" )
				.register();
		functionRegistry.registerAlternateKey( "every", "bool_and" );

		functionRegistry.namedAggregateDescriptorBuilder( "bool_or" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(BOOLEAN)
				.setInvariantType(booleanType)
				.setArgumentListSignature( "(BOOLEAN predicate)" )
				.register();
		functionRegistry.registerAlternateKey( "any", "bool_or" );
	}

	/**
	 * These are aggregate functions taking one argument,
	 * for databases that have to emulate the boolean
	 * aggregation functions using sum() and case.
	 */
	public void everyAny_sumCase(boolean supportsPredicateAsExpression) {
		functionRegistry.register( "every",
				new EveryAnyEmulation( typeConfiguration, true, supportsPredicateAsExpression ) );
		functionRegistry.register( "any",
				new EveryAnyEmulation( typeConfiguration, false, supportsPredicateAsExpression ) );
	}

	/**
	 * These are aggregate functions taking one argument,
	 * for SQL Server.
	 */
	public void everyAny_minMaxIif() {
		functionRegistry.register( "every",
				new SQLServerEveryAnyEmulation( typeConfiguration, true ) );
		functionRegistry.register( "any",
				new SQLServerEveryAnyEmulation( typeConfiguration, false ) );
	}


	/**
	 * These are aggregate functions taking one argument,
	 * for Oracle and Sybase.
	 */
	public void everyAny_minMaxCase() {
		functionRegistry.register( "every",
				new MinMaxCaseEveryAnyEmulation( typeConfiguration, true ) );
		functionRegistry.register( "any",
				new MinMaxCaseEveryAnyEmulation( typeConfiguration, false ) );
	}

	/**
	 * Note that we include these for completeness, but
	 * since their names collide with the HQL abbreviations
	 * for extract(), they can't actually be called from HQL.
	 */
	public void yearMonthDay() {
		functionRegistry.namedDescriptorBuilder( "day" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "month" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "year" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	/**
	 * Note that we include these for completeness, but
	 * since their names collide with the HQL abbreviations
	 * for extract(), they can't actually be called from HQL.
	 */
	public void hourMinuteSecond() {
		functionRegistry.namedDescriptorBuilder( "hour" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(TIME)
				.register();
		functionRegistry.namedDescriptorBuilder( "minute" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(TIME)
				.register();
		functionRegistry.namedDescriptorBuilder( "second" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(TIME)
				.register();
		functionRegistry.namedDescriptorBuilder( "microsecond" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(TIME)
				.register();
	}

	public void dayofweekmonthyear() {
		functionRegistry.namedDescriptorBuilder( "dayofweek" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "dayofmonth" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.registerAlternateKey( "day", "dayofmonth" );
		functionRegistry.namedDescriptorBuilder( "dayofyear" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	public void dayOfWeekMonthYear() {
		functionRegistry.namedDescriptorBuilder( "day_of_week" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "day_of_month" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.registerAlternateKey( "day", "day_of_month" );
		functionRegistry.namedDescriptorBuilder( "day_of_year" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	public void daynameMonthname() {
		functionRegistry.namedDescriptorBuilder( "monthname" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "dayname" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	public void weekQuarter() {
		functionRegistry.namedDescriptorBuilder( "week" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.namedDescriptorBuilder( "quarter" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.setInvariantType(integerType)
				.register();
	}

	public void lastDay() {
		functionRegistry.namedDescriptorBuilder( "last_day" )
				.setInvariantType(dateType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
	}

	public void lastDay_eomonth() {
		functionRegistry.namedDescriptorBuilder( "eomonth" )
				.setInvariantType(dateType)
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(DATE, INTEGER)
				.register();
		functionRegistry.registerAlternateKey( "last_date", "eomonth" );
	}

	public void ceiling_ceil() {
		functionRegistry.namedDescriptorBuilder( "ceil" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.register();
		functionRegistry.registerAlternateKey( "ceiling", "ceil" );
	}

	public void toCharNumberDateTimestamp() {
		//argument counts are right for Oracle, TimesTen, and CUBRID
		functionRegistry.namedDescriptorBuilder( "to_number" )
				//always 1 arg on HSQL and Cache, always 2 on Postgres
				.setArgumentCountBetween( 1, 3 )
				.setParameterTypes( STRING, STRING, STRING )
				.setInvariantType(doubleType)
				.register();
		functionRegistry.namedDescriptorBuilder( "to_char" )
				.setArgumentCountBetween( 1, 3 )
				.setParameterTypes( ANY, STRING, STRING )
				//always 2 args on HSQL and Postgres
				.setInvariantType(stringType)
				.register();
		functionRegistry.namedDescriptorBuilder( "to_date" )
				//always 2 args on HSQL and Postgres
				.setArgumentCountBetween( 1, 3 )
				.setParameterTypes( STRING, STRING, STRING )
				.setInvariantType(dateType)
				.register();
		functionRegistry.namedDescriptorBuilder( "to_timestamp" )
				//always 2 args on HSQL and Postgres
				.setArgumentCountBetween( 1, 3 )
				.setParameterTypes( STRING, STRING, STRING )
				.setInvariantType(timestampType)
				.register();
	}

	public void dateTimeTimestamp() {
		date();
		time();
		timestamp();
	}

	public void timestamp() {
		functionRegistry.namedDescriptorBuilder( "timestamp" )
				.setArgumentCountBetween( 1, 2 )
				//accepts (DATE,TIME) (DATE,INTEGER) or DATE or STRING
				.setInvariantType(timestampType)
				.register();
	}

	public void time() {
		functionRegistry.namedDescriptorBuilder( "time" )
				.setExactArgumentCount( 1 )
				//accepts TIME or STRING
				.setInvariantType(timeType)
				.register();
	}

	public void date() {
		functionRegistry.namedDescriptorBuilder( "date" )
				.setExactArgumentCount( 1 )
				//accepts DATE or STRING
				.setInvariantType(dateType)
				.register();
	}

	public void utcDateTimeTimestamp() {
		functionRegistry.noArgsBuilder( "utc_date" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(dateType)
				.register();
		functionRegistry.noArgsBuilder( "utc_time" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(timeType)
				.register();
		functionRegistry.noArgsBuilder( "utc_timestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(timestampType)
				.register();
	}

	public void currentUtcdatetimetimestamp() {
		functionRegistry.noArgsBuilder( "current_utcdate" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(dateType)
				.register();
		functionRegistry.noArgsBuilder( "current_utctime" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(timeType)
				.register();
		functionRegistry.noArgsBuilder( "current_utctimestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType(timestampType)
				.register();
	}

	public void week_weekofyear() {
		functionRegistry.namedDescriptorBuilder( "weekofyear" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(DATE)
				.register();
		functionRegistry.registerAlternateKey( "week", "weekofyear" );
	}

	/**
	 * Almost every database
	 */
	public void concat_pipeOperator() {
		functionRegistry.patternDescriptorBuilder( "concat", "(?1||?2...)" )
				.setInvariantType(stringType)
				.setMinArgumentCount( 1 )
				.setArgumentTypeResolver(
						StandardFunctionArgumentTypeResolvers.impliedOrInvariant( typeConfiguration, STRING )
				)
				.setArgumentListSignature( "(STRING string0[, STRING string1[, ...]])" )
				.register();
	}

	public void concat_pipeOperator(String clobPattern) {
		functionRegistry.register( "concat", new ConcatPipeFunction( clobPattern, typeConfiguration ) );
	}

	/**
	 * Oracle-style
	 */
	public void rownumRowid() {
		functionRegistry.noArgsBuilder( "rowid" )
				.setInvariantType(longType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
		functionRegistry.noArgsBuilder( "rownum" )
				.setInvariantType(longType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	/**
	 * H2/HSQL-style
	 */
	public void rownum() {
		functionRegistry.noArgsBuilder( "rownum" )
				.setInvariantType(longType)
				.setUseParenthesesWhenNoArgs( true ) //H2 and HSQL require the parens
				.register();
	}

	/**
	 * CUBRID
	 */
	public void rownumInstOrderbyGroupbyNum() {
		functionRegistry.noArgsBuilder( "rownum" )
				.setInvariantType(integerType)
				.setUseParenthesesWhenNoArgs( false )
				.register();

		functionRegistry.noArgsBuilder( "inst_num" )
				.setInvariantType(integerType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
		functionRegistry.noArgsBuilder( "orderby_num" )
				.setInvariantType(integerType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
		functionRegistry.noArgsBuilder( "groupby_num" )
				.setInvariantType(integerType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	/**
	 * MySQL/CUBRID
	 */
	public void makedateMaketime() {
		functionRegistry.namedDescriptorBuilder( "makedate" )
				.setInvariantType(dateType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(INTEGER, INTEGER)
				.setArgumentListSignature( "(INTEGER year, INTEGER dayofyear)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "maketime" )
				.setInvariantType(timeType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER)
				.setArgumentListSignature( "(INTEGER hour, INTEGER min, INTEGER sec)" )
				.register();
	}

	/**
	 * Postgres
	 */
	public void makeDateTimeTimestamp() {
		functionRegistry.namedDescriptorBuilder( "make_date" )
				.setInvariantType(dateType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER)
				.register();
		functionRegistry.namedDescriptorBuilder( "make_time" )
				.setInvariantType(timeType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER)
				.register();
		functionRegistry.namedDescriptorBuilder( "make_timestamp" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 6 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER)
				.register();
		functionRegistry.namedDescriptorBuilder( "make_timestamptz" )
				.setInvariantType( timestampType )
				.setArgumentCountBetween( 6, 7 )
				.setParameterTypes(INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER, INTEGER)
				.register();
	}

	public void sysdate() {
		// returns a local timestamp
		functionRegistry.noArgsBuilder( "sysdate" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	/**
	 * MySQL requires the parens in sysdate()
	 */
	public void sysdateParens() {
		functionRegistry.noArgsBuilder( "sysdate" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	/**
	 * MySQL 5.7 precision defaults to seconds, but microseconds is better
	 */
	public void sysdateExplicitMicros() {
		functionRegistry.patternDescriptorBuilder( "sysdate", "sysdate(6)" )
				.setInvariantType(timestampType)
				.setExactArgumentCount( 0 )
				.register();
	}

	public void systimestamp() {
		// returns a timestamp with timezone
		functionRegistry.noArgsBuilder( "systimestamp" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	public void localtimeLocaltimestamp() {
		//these functions return times without timezones
		functionRegistry.noArgsBuilder( "localtime" )
				.setInvariantType(timeType)
				.setUseParenthesesWhenNoArgs( false )
				.register();
		functionRegistry.noArgsBuilder( "localtimestamp" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( false )
				.register();

		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		functionRegistry.noArgsBuilder( "local_time", "localtime" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_TIME ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
		functionRegistry.noArgsBuilder( "local_datetime", "localtimestamp" )
				.setInvariantType ( basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_DATE_TIME ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	public void trigonometry() {
		functionRegistry.namedDescriptorBuilder( "sin" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "cos" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "tan" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "asin" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "acos" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "atan" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "atan2" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	/**
	 * Transact-SQL atan2 is misspelled
	 */
	public void atan2_atn2() {
		functionRegistry.namedDescriptorBuilder( "atan2", "atn2" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void coalesce() {
		functionRegistry.namedDescriptorBuilder( "coalesce" )
				.setMinArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
	}

	/**
	 * SAP DB
	 */
	public void coalesce_value() {
		functionRegistry.namedDescriptorBuilder( "value" )
				.setMinArgumentCount( 1 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.registerAlternateKey( "coalesce", "value" );
	}

	public void nullif() {
		functionRegistry.namedDescriptorBuilder( "nullif" )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
	}

	/**
	 * ANSI SQL-style
	 */
	public void length_characterLength() {
		functionRegistry.namedDescriptorBuilder( "character_length" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING_OR_CLOB)
				.register();
		functionRegistry.registerAlternateKey( "length", "character_length" );
	}

	public void length_characterLength_pattern(String clobPattern) {
		functionRegistry.register(
				"character_length",
				new LengthFunction( "character_length", "character_length(?1)", clobPattern, typeConfiguration )
		);
		functionRegistry.registerAlternateKey( "length", "character_length" );
	}

	/**
	 * Transact SQL-style
	 */
	public void characterLength_len() {
		functionRegistry.namedDescriptorBuilder( "character_length", "len" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING_OR_CLOB)
				.register();
		functionRegistry.registerAlternateKey( "len", "character_length" );
		functionRegistry.registerAlternateKey( "length", "character_length" );
	}

	/**
	 * Oracle-style
	 */
	public void characterLength_length(SqlAstNodeRenderingMode argumentRenderingMode) {
		functionRegistry.namedDescriptorBuilder( "character_length", "length" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING_OR_CLOB)
				.setArgumentRenderingMode( argumentRenderingMode )
				.register();
		functionRegistry.registerAlternateKey( "length", "character_length" );
	}

	public void characterLength_length(String clobPattern) {
		functionRegistry.register(
				"character_length",
				new LengthFunction( "length", "length(?1)", clobPattern, typeConfiguration )
		);
		functionRegistry.registerAlternateKey( "length", "character_length" );
	}

	public void octetLength() {
		functionRegistry.namedDescriptorBuilder( "octet_length" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING_OR_CLOB)
				.register();
	}

	public void octetLength_pattern(String pattern) {
		octetLength_pattern( pattern, SqlAstNodeRenderingMode.DEFAULT );
	}

	public void octetLength_pattern(String pattern, SqlAstNodeRenderingMode renderingMode) {
		functionRegistry.patternDescriptorBuilder( "octet_length", pattern )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING_OR_CLOB)
				.setArgumentRenderingMode( renderingMode )
				.register();
	}

	public void octetLength_pattern(String pattern, String clobPattern) {
		functionRegistry.register(
				"octet_length",
				new LengthFunction( "octet_length", pattern, clobPattern, typeConfiguration )
		);
	}

	public void bitLength() {
		functionRegistry.namedDescriptorBuilder( "bit_length" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING_OR_CLOB)
				.register();
	}

	public void bitLength_pattern(String pattern) {
		bitLength_pattern( pattern, SqlAstNodeRenderingMode.DEFAULT );
	}

	public void bitLength_pattern(String pattern, SqlAstNodeRenderingMode renderingMode) {
		functionRegistry.patternDescriptorBuilder( "bit_length", pattern )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING_OR_CLOB)
				.setArgumentRenderingMode( renderingMode )
				.register();
	}

	public void bitLength_pattern(String pattern, String clobPattern) {
		functionRegistry.register(
				"bit_length",
				new LengthFunction( "bit_length", pattern, clobPattern, typeConfiguration )
		);
	}

	/**
	 * ANSI-style
	 */
	public void position() {
		functionRegistry.patternDescriptorBuilder( "position", "position(?1 in ?2)" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, STRING)
				.setArgumentListSignature( "(STRING pattern in STRING string)" )
				.register();
	}

	public void locate() {
		functionRegistry.namedDescriptorBuilder( "locate" )
				.setInvariantType(integerType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, STRING, INTEGER)
				.setArgumentListSignature( "(STRING pattern, STRING string[, INTEGER start])" )
				.register();
	}

	/**
	 * Transact SQL-style
	 */
	public void locate_charindex() {
		functionRegistry.namedDescriptorBuilder( "charindex" )
				.setInvariantType(integerType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, STRING, INTEGER)
				.setArgumentListSignature( "(STRING pattern, STRING string[, INTEGER start])" )
				.register();
		functionRegistry.registerAlternateKey( "locate", "charindex" );
	}

	/**
	 * locate() in terms of ANSI position() and substring()
	 */
	public void locate_positionSubstring() {
		functionRegistry.registerBinaryTernaryPattern(
						"locate",
						integerType,
						"position(?1 in ?2)", "(position(?1 in substring(?2 from ?3))+(?3)-1)",
						STRING, STRING, INTEGER,
						typeConfiguration
				)
				.setArgumentListSignature( "(STRING pattern, STRING string[, INTEGER start])" );
	}
	/**
	 * ANSI-style substring
	 */
	public void substringFromFor() {
		functionRegistry.registerBinaryTernaryPattern(
						"substring",
						stringType,
						"substring(?1 from ?2)", "substring(?1 from ?2 for ?3)",
						STRING, INTEGER, INTEGER,
						typeConfiguration
				)
				.setArgumentListSignature( "(STRING string{ from|,} INTEGER start[{ for|,} INTEGER length])" );
	}

	/**
	 * Not the same as ANSI-style substring!
	 */
	public void substring() {
		functionRegistry.namedDescriptorBuilder( "substring" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, INTEGER)
				.setArgumentListSignature( "(STRING string{ from|,} INTEGER start[{ for|,} INTEGER length])" )
				.register();
	}

	/**
	 * Transact SQL-style (3 required args)
	 */
	public void substring_substringLen() {
		functionRegistry
				.registerBinaryTernaryPattern(
						"substring",
						stringType,
						"substring(?1,?2,len(?1)-?2+1)",
						"substring(?1,?2,?3)",
						STRING, INTEGER, INTEGER,
						typeConfiguration
				)
				.setArgumentListSignature( "(STRING string{ from|,} INTEGER start[{ for|,} INTEGER length])" );
	}

	/**
	 * Oracle, and many others
	 */
	public void substring_substr() {
		functionRegistry.namedDescriptorBuilder( "substring", "substr" )
				.setArgumentListSignature( "(STRING string{ from|,} INTEGER start[{ for|,} INTEGER length])" )
				.setInvariantType(stringType)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(STRING, INTEGER, INTEGER)
				.register();
	}

	public void insert() {
		functionRegistry.namedDescriptorBuilder( "insert" )
				.setInvariantType(stringType)
				.setParameterTypes(STRING, INTEGER, INTEGER, STRING)
				.setArgumentListSignature( "(STRING string, INTEGER start, INTEGER length, STRING replacement)" )
				.register();
	}

	/**
	 * Postgres
	 */
	public void insert_overlay() {
		functionRegistry.patternDescriptorBuilder(
						"insert",
						"overlay(?1 placing ?4 from ?2 for ?3)"
				)
				.setInvariantType(stringType)
				.setExactArgumentCount( 4 )
				.setParameterTypes(STRING, INTEGER, INTEGER, STRING)
				.setArgumentListSignature( "(STRING string, INTEGER start, INTEGER length, STRING replacement)" )
				.register();
	}

	/**
	 * ANSI SQL form, supported by Postgres, HSQL
	 */
	public void overlay() {
		functionRegistry.registerTernaryQuaternaryPattern(
						"overlay",
						stringType,
						"overlay(?1 placing ?2 from ?3)",
						"overlay(?1 placing ?2 from ?3 for ?4)",
						STRING, STRING, INTEGER, INTEGER,
						typeConfiguration
				)
				.setArgumentListSignature( "(string placing replacement from start[ for length])" );
	}

	/**
	 * For DB2 which has a broken implementation of overlay()
	 */
	public void overlayLength_overlay(boolean withCodeUnits) {
		final String codeUnits = withCodeUnits ? " using codeunits32" : "";
		functionRegistry.registerTernaryQuaternaryPattern(
						"overlay",
						stringType,
						"overlay(?1 placing ?2 from ?3 for character_length(?2" + (withCodeUnits ? ",codeunits32" : "") + ")" + codeUnits + ")",
						"overlay(?1 placing ?2 from ?3 for ?4" + codeUnits + ")",
						STRING, STRING, INTEGER, INTEGER,
						typeConfiguration
				)
				.setArgumentListSignature( "(string placing replacement from start[ for length])" );
	}

	public void replace() {
		functionRegistry.namedDescriptorBuilder( "replace" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(STRING, STRING, STRING)
				.setArgumentListSignature( "(STRING string, STRING pattern, STRING replacement)" )
				.register();
	}

	/**
	 * Sybase
	 */
	public void replace_strReplace() {
		functionRegistry.namedDescriptorBuilder( "str_replace" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 3 )
				.setParameterTypes(STRING, STRING, STRING)
				.setArgumentListSignature( "(STRING string, STRING pattern, STRING replacement)" )
				.register();
		functionRegistry.registerAlternateKey( "replace", "str_replace" );
	}

	public void concat() {
		functionRegistry.namedDescriptorBuilder( "concat" )
				.setInvariantType(stringType)
				.setMinArgumentCount( 1 )
				.setArgumentTypeResolver(
						StandardFunctionArgumentTypeResolvers.impliedOrInvariant( typeConfiguration, STRING )
				)
				.setArgumentListSignature( "(STRING string0[, STRING string1[, ...]])" )
				.register();
	}

	public void lowerUpper() {
		functionRegistry.namedDescriptorBuilder( "lower" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "upper" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setArgumentListSignature( "(STRING string)" )
				.register();
	}

	/**
	 * Very widely supported, but we don't treat this as a "standard"
	 * function because it's hard to emulate on any database that
	 * doesn't have it (e.g. Derby) and because, well, ASCII. For the
	 * same reason we don't consider chr()/char() as "standard".
	 */
	public void ascii() {
		functionRegistry.namedDescriptorBuilder( "ascii" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(STRING)
				.setInvariantType(integerType)//should it be BYTE??
				.register();
	}

	public void char_chr() {
		functionRegistry.namedDescriptorBuilder( "chr" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(INTEGER)
				.setInvariantType(characterType)
				.register();
		functionRegistry.registerAlternateKey( "char", "chr" );
	}

	public void chr_char() {
		functionRegistry.namedDescriptorBuilder( "char" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(INTEGER)
				.setInvariantType(characterType)
				.register();
		functionRegistry.registerAlternateKey( "chr", "char" );
	}

	/**
	 * Transact SQL-style
	 */
	public void datepartDatename() {
		functionRegistry.namedDescriptorBuilder( "datepart" )
//				.setInvariantType( StandardBasicTypes.INTEGER )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TEMPORAL_UNIT, TEMPORAL)
				.setArgumentListSignature( "(TEMPORAL_UNIT field, TEMPORAL arg)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "datename" )
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TEMPORAL_UNIT, TEMPORAL)
				.setArgumentListSignature( "(TEMPORAL_UNIT field, TEMPORAL arg)" )
				.register();
	}

	// No real consistency in the semantics of these functions:
	// H2, HSQL: now()/curtime()/curdate() mean localtimestamp/localtime/current_date
	// MySQL, Cache: now()/curtime()/curdate() mean current_timestamp/current_time/current_date
	// CUBRID: now()/curtime()/curdate() mean current_datetime/current_time/current_date
	// Postgres: now() means current_timestamp
	public void nowCurdateCurtime() {
		functionRegistry.noArgsBuilder( "curtime" )
				.setInvariantType(timeType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
		functionRegistry.noArgsBuilder( "curdate" )
				.setInvariantType(dateType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
		functionRegistry.noArgsBuilder( "now" )
				.setInvariantType(timestampType)
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	public void leastGreatest() {
		functionRegistry.namedDescriptorBuilder( "least" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.namedDescriptorBuilder( "greatest" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
	}

	public void leastGreatest_minMax() {
		functionRegistry.namedDescriptorBuilder( "least", "min" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.namedDescriptorBuilder( "greatest", "max" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
	}

	public void leastGreatest_minMaxValue() {
		functionRegistry.namedDescriptorBuilder( "least", "minvalue" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
		functionRegistry.namedDescriptorBuilder( "greatest", "maxvalue" )
				.setMinArgumentCount( 2 )
				.setParameterTypes(COMPARABLE, COMPARABLE)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();
	}

	public void aggregates(Dialect dialect, SqlAstNodeRenderingMode inferenceArgumentRenderingMode) {
		functionRegistry.namedAggregateDescriptorBuilder( "max" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setExactArgumentCount( 1 )
				.setParameterTypes(COMPARABLE)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.register();

		functionRegistry.namedAggregateDescriptorBuilder( "min" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setExactArgumentCount( 1 )
				.setParameterTypes(COMPARABLE)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.register();

		functionRegistry.namedAggregateDescriptorBuilder( "sum" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setReturnTypeResolver( new SumReturnTypeResolver( typeConfiguration ) )
				.setExactArgumentCount( 1 )
				.register();


		functionRegistry.namedAggregateDescriptorBuilder( "avg" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setArgumentsValidator( AvgFunction.Validator.INSTANCE )
				.setReturnTypeResolver( new AvgFunction.ReturnTypeResolver( typeConfiguration ) )
				.register();

		functionRegistry.register(
				"count",
				new CountFunction(
						dialect,
						typeConfiguration,
						inferenceArgumentRenderingMode,
						"||"
				)
		);
	}

	public void avg_castingNonDoubleArguments(
			Dialect dialect,
			SqlAstNodeRenderingMode inferenceArgumentRenderingMode) {
		functionRegistry.register(
				"avg",
				new AvgFunction(
						dialect,
						typeConfiguration,
						inferenceArgumentRenderingMode
				)
		);
	}

	public void listagg(String emptyWithinReplacement) {
		functionRegistry.register(
				"listagg",
				new ListaggFunction( emptyWithinReplacement, typeConfiguration )
		);
	}

	public void listagg_groupConcat() {
		functionRegistry.register(
				ListaggGroupConcatEmulation.FUNCTION_NAME,
				new ListaggGroupConcatEmulation( typeConfiguration )
		);
	}

	public void listagg_list(String stringType) {
		functionRegistry.register(
				ListaggStringAggEmulation.FUNCTION_NAME,
				new ListaggStringAggEmulation( "list", stringType, false, typeConfiguration )
		);
	}

	public void listagg_stringAgg(String stringType) {
		functionRegistry.register(
				ListaggStringAggEmulation.FUNCTION_NAME,
				new ListaggStringAggEmulation( "string_agg", stringType, false, typeConfiguration )
		);
	}

	public void listagg_stringAggWithinGroup(String stringType) {
		functionRegistry.register(
				ListaggStringAggEmulation.FUNCTION_NAME,
				new ListaggStringAggEmulation( "string_agg", stringType, true, typeConfiguration )
		);
	}

	public void inverseDistributionOrderedSetAggregates() {
		functionRegistry.register(
				"mode",
				new InverseDistributionFunction( "mode", null, typeConfiguration )
		);
		functionRegistry.register(
				"percentile_cont",
				new InverseDistributionFunction( "percentile_cont", NUMERIC, typeConfiguration )
		);
		functionRegistry.register(
				"percentile_disc",
				new InverseDistributionFunction( "percentile_disc", NUMERIC, typeConfiguration )
		);
	}

	public void inverseDistributionOrderedSetAggregates_windowEmulation() {
		functionRegistry.register(
				"percentile_cont",
				new InverseDistributionWindowEmulation( "percentile_cont", NUMERIC, typeConfiguration )
		);
		functionRegistry.register(
				"percentile_disc",
				new InverseDistributionWindowEmulation( "percentile_disc", NUMERIC, typeConfiguration )
		);
	}

	public void hypotheticalOrderedSetAggregates() {
		functionRegistry.register(
				"rank",
				new HypotheticalSetFunction( "rank", StandardBasicTypes.LONG, typeConfiguration )
		);
		functionRegistry.register(
				"dense_rank",
				new HypotheticalSetFunction( "dense_rank", StandardBasicTypes.LONG, typeConfiguration )
		);
		functionRegistry.register(
				"percent_rank",
				new HypotheticalSetFunction( "percent_rank", StandardBasicTypes.DOUBLE, typeConfiguration )
		);
		functionRegistry.register(
				"cume_dist",
				new HypotheticalSetFunction( "cume_dist", StandardBasicTypes.DOUBLE, typeConfiguration )
		);
	}

	public void hypotheticalOrderedSetAggregates_windowEmulation() {
		functionRegistry.register(
				"rank",
				new HypotheticalSetWindowEmulation( "rank", StandardBasicTypes.LONG, typeConfiguration )
		);
		functionRegistry.register(
				"dense_rank",
				new HypotheticalSetWindowEmulation( "dense_rank", StandardBasicTypes.LONG, typeConfiguration )
		);
		functionRegistry.register(
				"percent_rank",
				new HypotheticalSetWindowEmulation( "percent_rank", StandardBasicTypes.DOUBLE, typeConfiguration )
		);
		functionRegistry.register(
				"cume_dist",
				new HypotheticalSetWindowEmulation( "cume_dist", StandardBasicTypes.DOUBLE, typeConfiguration )
		);
	}

	public void windowFunctions() {
		functionRegistry.namedWindowDescriptorBuilder( "row_number" )
				.setExactArgumentCount( 0 )
				.setInvariantType( longType )
				.register();
		functionRegistry.namedWindowDescriptorBuilder( "lag" )
				.setArgumentCountBetween( 1, 3 )
				.setParameterTypes( ANY, INTEGER, ANY )
				.setArgumentTypeResolver(
						StandardFunctionArgumentTypeResolvers.byArgument(
								StandardFunctionArgumentTypeResolvers.argumentsOrImplied( 2 ),
								StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, INTEGER ),
								StandardFunctionArgumentTypeResolvers.argumentsOrImplied( 0 )
						)
				)
				.setArgumentListSignature( "ANY value[, INTEGER offset[, ANY default]]" )
				.register();
		functionRegistry.namedWindowDescriptorBuilder( "lead" )
				.setArgumentCountBetween( 1, 3 )
				.setParameterTypes( ANY, INTEGER, ANY )
				.setArgumentTypeResolver(
						StandardFunctionArgumentTypeResolvers.byArgument(
								StandardFunctionArgumentTypeResolvers.argumentsOrImplied( 2 ),
								StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, INTEGER ),
								StandardFunctionArgumentTypeResolvers.argumentsOrImplied( 0 )
						)
				)
				.setArgumentListSignature( "ANY value[, INTEGER offset[, ANY default]]" )
				.register();
		functionRegistry.namedWindowDescriptorBuilder( "first_value" )
				.setExactArgumentCount( 1 )
				.setParameterTypes( ANY )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.setArgumentListSignature( "ANY value" )
				.register();
		functionRegistry.namedWindowDescriptorBuilder( "last_value" )
				.setExactArgumentCount( 1 )
				.setParameterTypes( ANY )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.setArgumentListSignature( "ANY value" )
				.register();
		functionRegistry.namedWindowDescriptorBuilder( "nth_value" )
				.setExactArgumentCount( 2 )
				.setParameterTypes( ANY, INTEGER )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE )
				.setArgumentListSignature( "ANY value, INTEGER nth" )
				.register();
	}

	public void math() {
		functionRegistry.namedDescriptorBuilder( "floor" )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "ceiling" )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "mod" )
				// According to JPA spec 4.6.17.2.2.
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(INTEGER, INTEGER)
				.register();

		functionRegistry.namedDescriptorBuilder( "abs" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "sign" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		//transcendental functions are by nature of floating point type

		functionRegistry.namedDescriptorBuilder( "sqrt" )
				// According to JPA spec 4.6.17.2.2.
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "ln" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "exp" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();

		functionRegistry.namedDescriptorBuilder( "power" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void mod_operator() {
		functionRegistry.patternDescriptorBuilder( "mod", "(?1%?2)" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(INTEGER, INTEGER)
				.register();
	}

	public void power_expLn() {
		functionRegistry.patternDescriptorBuilder( "power", "exp(ln(?1)*?2)" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(NUMERIC, NUMERIC)
				.register();
	}

	public void round() {
		functionRegistry.namedDescriptorBuilder( "round" )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.setArgumentCountBetween( 1, 2 )
				.setParameterTypes(NUMERIC, INTEGER)
				.setArgumentListSignature( "(NUMERIC number[, INTEGER places])" )
				.register();
	}

	/**
	 * SQL Server
	 */
	public void round_round() {
		functionRegistry.registerUnaryBinaryPattern(
				"round",
				"round(?1,0)",
				"round(?1,?2)",
				NUMERIC, INTEGER,
				typeConfiguration
		).setArgumentListSignature( "(NUMERIC number[, INTEGER places])" );
	}

	/**
	 * Derby (only works if the second arg is constant, as it almost always is)
	 */
	public void round_floor() {
		functionRegistry.registerUnaryBinaryPattern(
				"round",
				"floor(?1+0.5)",
				"floor(?1*1e?2+0.5)/1e?2",
				NUMERIC, INTEGER,
				typeConfiguration
		).setArgumentListSignature( "(NUMERIC number[, INTEGER places])" );
	}

	/**
	 * PostgreSQL (only works if the second arg is constant, as it almost always is)
	 */
	public void round_roundFloor() {
		functionRegistry.registerUnaryBinaryPattern(
				"round",
				"round(?1)",
				"floor(?1*1e?2+0.5)/1e?2",
				NUMERIC, INTEGER,
				typeConfiguration
		).setArgumentListSignature( "(NUMERIC number[, INTEGER places])" );
	}

	public void square() {
		functionRegistry.namedDescriptorBuilder( "square" )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	public void cbrt() {
		functionRegistry.namedDescriptorBuilder( "cbrt" )
				.setInvariantType(doubleType)
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	@Deprecated(since = "7")
	public void crc32() {
		functionRegistry.namedDescriptorBuilder( "crc32" )
				.setInvariantType(integerType)
				.setParameterTypes( STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public void hex(String pattern) {
		functionRegistry.patternDescriptorBuilder( "hex", pattern )
				.setInvariantType(stringType)
				.setParameterTypes( BINARY )
				.setExactArgumentCount( 1 )
				.register();
	}

	public void md5(String pattern) {
		functionRegistry.patternDescriptorBuilder( "md5", pattern )
				.setInvariantType(binaryType)
				.setParameterTypes( STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public void sha(String pattern) {
		functionRegistry.patternDescriptorBuilder( "sha", pattern )
				.setInvariantType(binaryType)
				.setParameterTypes( STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	@Deprecated(since = "7")
	public void sha1() {
		functionRegistry.namedDescriptorBuilder( "sha1" )
				.setInvariantType(stringType)
				.setParameterTypes( STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	@Deprecated(since = "7")
	public void sha2() {
		functionRegistry.namedDescriptorBuilder( "sha2" )
				.setInvariantType(stringType)
				.setParameterTypes( STRING, INTEGER )
				.setExactArgumentCount( 2 )
				.register();
	}

	@Deprecated(since = "7")
	public void sha() {
		functionRegistry.namedDescriptorBuilder( "sha" )
				.setInvariantType(stringType)
				.setParameterTypes( STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public void timestampaddAndDiff(Dialect dialect, SqlAstNodeRenderingMode timestampRenderingMode) {
		functionRegistry.register(
				"timestampadd",
				new TimestampaddFunction(
						dialect,
						typeConfiguration,
						SqlAstNodeRenderingMode.DEFAULT,
						SqlAstNodeRenderingMode.DEFAULT,
						timestampRenderingMode
				)
		);
		functionRegistry.register(
				"timestampdiff",
				new TimestampdiffFunction(
						dialect,
						typeConfiguration,
						SqlAstNodeRenderingMode.DEFAULT,
						timestampRenderingMode,
						timestampRenderingMode
				)
		);
	}

	/**
	 * MySQL style, returns the number of days between two dates
	 */
	public void datediff() {
		functionRegistry.namedDescriptorBuilder( "datediff" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
	}

	/**
	 * MySQL style
	 */
	public void adddateSubdateAddtimeSubtime() {
		functionRegistry.namedDescriptorBuilder( "adddate" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER days)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "subdate" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER days)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "addtime" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME datetime, TIME time)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "subtime" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME datetime, TIME time)" )
				.register();
	}

	public void addMonths() {
		functionRegistry.namedDescriptorBuilder( "add_months" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setArgumentListSignature( "(DATE datetime, INTEGER months)" )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.register();
	}

	public void monthsBetween() {
		functionRegistry.namedDescriptorBuilder( "months_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.setParameterTypes(DATE, DATE)
				.register();
	}

	public void daysBetween() {
		functionRegistry.namedDescriptorBuilder( "days_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
	}

	public void secondsBetween() {
		functionRegistry.namedDescriptorBuilder( "seconds_between" )
				.setInvariantType(longType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME end, TIME start)" )
				.register();
	}

	public void yearsMonthsDaysHoursMinutesSecondsBetween() {
		functionRegistry.namedDescriptorBuilder( "years_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "months_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "days_between" )
				.setInvariantType(integerType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, DATE)
				.setArgumentListSignature( "(DATE end, DATE start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "hours_between" )
				.setInvariantType(longType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME end, TIME start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "minutes_between" )
				.setInvariantType(longType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME end, TIME start)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "seconds_between" )
				.setInvariantType(longType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, TIME)
				.setArgumentListSignature( "(TIME end, TIME start)" )
				.register();
	}

	public void addYearsMonthsDaysHoursMinutesSeconds() {
		functionRegistry.namedDescriptorBuilder( "add_years" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER years)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_months" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER months)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_days" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(DATE, INTEGER)
				.setArgumentListSignature( "(DATE datetime, INTEGER days)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_hours" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, INTEGER)
				.setArgumentListSignature( "(TIME datetime, INTEGER hours)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_minutes" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, INTEGER)
				.setArgumentListSignature( "(TIME datetime, INTEGER minutes)" )
				.register();
		functionRegistry.namedDescriptorBuilder( "add_seconds" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes(TIME, INTEGER)
				.setArgumentListSignature( "(TIME datetime, INTEGER seconds)" )
				.register();
	}

	/**
	 * H2-style (uses Java's SimpleDateFormat directly so no need to translate format)
	 */
	public void format_formatdatetime() {
		functionRegistry.register( "format", new FormatFunction( "formatdatetime", typeConfiguration ) );
	}

	/**
	 * Usually Oracle-style (except for Informix which quite close to MySQL-style)
	 *
	 * @see org.hibernate.dialect.OracleDialect#datetimeFormat
	 */
	public void format_toChar() {
		functionRegistry.register( "format", new FormatFunction( "to_char", typeConfiguration ) );
	}

	/**
	 * Usually Oracle-style (except for Informix which quite close to MySQL-style)
	 *
	 * @see org.hibernate.dialect.OracleDialect#datetimeFormat
	 */
	public void format_toChar_gauss() {
		functionRegistry.register( "format", new GaussDBFormatFunction( "to_char", typeConfiguration ) );
	}

	/**
	 * MySQL-style (also Ingres)
	 *
	 * @see org.hibernate.dialect.MySQLDialect#datetimeFormat
	 */
	public void format_dateFormat() {
		functionRegistry.register( "format", new FormatFunction( "date_format", typeConfiguration ) );
	}

	/**
	 * HANA's name for to_char() is still Oracle-style
	 *
	 *  @see org.hibernate.dialect.OracleDialect#datetimeFormat
	 */
	public void format_toVarchar() {
		functionRegistry.register( "format", new FormatFunction( "to_varchar", typeConfiguration ) );
	}

	/**
	 * Use the 'collate' operator which exists on at least Postgres, MySQL, Oracle, and SQL Server
	 */
	public void collate() {
		functionRegistry.patternDescriptorBuilder("collate", "(?1 collate ?2)")
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, COLLATION)
				.setArgumentListSignature("(STRING string as COLLATION collation)")
				.register();
	}

	/**
	 * HSQL requires quotes around certain collations
	 */
	public void collate_quoted() {
		functionRegistry.patternDescriptorBuilder("collate", "(?1 collate '?2')")
				.setInvariantType(stringType)
				.setExactArgumentCount( 2 )
				.setParameterTypes(STRING, COLLATION)
				.setArgumentListSignature("(STRING string as COLLATION collation)")
				.register();
	}

	/**
	 * H2, DB2 and PostgreSQL native date_trunc() function
	 */
	public void dateTrunc() {
		functionRegistry.patternDescriptorBuilder( "date_trunc", "date_trunc(?1,?2)" )
				.setReturnTypeResolver( useArgType( 2 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes( STRING, TEMPORAL )
				.setArgumentListSignature( "(STRING field, TEMPORAL datetime)" )
				.register();
	}

	/**
	 * SQLServer native datetrunc() function
	 */
	public void dateTrunc_datetrunc() {
		functionRegistry.patternDescriptorBuilder( "datetrunc", "datetrunc(?1,?2)" )
				.setReturnTypeResolver( useArgType( 2 ) )
				.setExactArgumentCount( 2 )
				.setParameterTypes( TEMPORAL_UNIT, TEMPORAL )
				.setArgumentListSignature( "(TEMPORAL_UNIT field, TEMPORAL datetime)" )
				.register();
	}

	/**
	 * H2, HSQL array() constructor function
	 */
	public void array() {
		functionRegistry.register( "array", new ArrayConstructorFunction( false, true ) );
		functionRegistry.register( "array_list", new ArrayConstructorFunction( true, true ) );
	}

	/**
	 * H2, HSQL array() constructor function
	 */
	public void array_hsql() {
		functionRegistry.register( "array", new HSQLArrayConstructorFunction( false ) );
		functionRegistry.register( "array_list", new HSQLArrayConstructorFunction( true ) );
	}

	/**
	 * CockroachDB and PostgreSQL array() constructor function
	 */
	public void array_postgresql() {
		functionRegistry.register( "array", new PostgreSQLArrayConstructorFunction( false ) );
		functionRegistry.register( "array_list", new PostgreSQLArrayConstructorFunction( true ) );
	}

	/**
	 * GaussDB array() constructor function
	 */
	public void array_gaussdb() {
		functionRegistry.register( "array", new GaussDBArrayConstructorFunction( false ) );
		functionRegistry.register( "array_list", new GaussDBArrayConstructorFunction( true ) );
	}

	/**
	 * Google Spanner array() constructor function
	 */
	public void array_spanner() {
		functionRegistry.register( "array", new ArrayConstructorFunction( false, false ) );
		functionRegistry.register( "array_list", new ArrayConstructorFunction( true, false ) );
	}

	/**
	 * Oracle array() constructor function
	 */
	public void array_oracle() {
		functionRegistry.register( "array", new OracleArrayConstructorFunction( false ) );
		functionRegistry.register( "array_list", new OracleArrayConstructorFunction( true ) );
	}

	/**
	 * H2, HSQL, CockroachDB and PostgreSQL array_agg() function
	 */
	public void arrayAggregate() {
		functionRegistry.register( ArrayAggFunction.FUNCTION_NAME, new ArrayAggFunction( "array_agg", false, true ) );
	}

	/**
	 * Oracle array_agg() function
	 */
	public void arrayAggregate_jsonArrayagg() {
		functionRegistry.register( ArrayAggFunction.FUNCTION_NAME, new OracleArrayAggEmulation() );
	}

	/**
	 * H2 array_contains() function
	 */
	public void arrayContains_h2(int maximumArraySize) {
		functionRegistry.register(
				"array_contains",
				new H2ArrayContainsFunction( false, maximumArraySize, typeConfiguration )
		);
		functionRegistry.register(
				"array_contains_nullable",
				new H2ArrayContainsFunction( true, maximumArraySize, typeConfiguration )
		);
		functionRegistry.register(
				"array_includes",
				new H2ArrayIncludesFunction( false, maximumArraySize, typeConfiguration )
		);
		functionRegistry.register(
				"array_includes_nullable",
				new H2ArrayIncludesFunction( true, maximumArraySize, typeConfiguration )
		);
	}

	/**
	 * HSQL array_contains() function
	 */
	public void arrayContains_hsql() {
		functionRegistry.register(
				"array_contains",
				new ArrayContainsUnnestFunction( false, typeConfiguration )
		);
		functionRegistry.register(
				"array_contains_nullable",
				new ArrayContainsUnnestFunction( true, typeConfiguration )
		);
		functionRegistry.register(
				"array_includes",
				new ArrayIncludesUnnestFunction( false, typeConfiguration )
		);
		functionRegistry.register(
				"array_includes_nullable",
				new ArrayIncludesUnnestFunction( true, typeConfiguration )
		);
	}

	/**
	 * CockroachDB and PostgreSQL array contains operator
	 */
	public void arrayContains_postgresql() {
		functionRegistry.register( "array_contains", new ArrayContainsOperatorFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_contains_nullable", new ArrayContainsOperatorFunction( true, typeConfiguration ) );
		functionRegistry.register( "array_includes", new ArrayIncludesOperatorFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_includes_nullable", new ArrayIncludesOperatorFunction( true, typeConfiguration ) );
	}

	/**
	 * GaussDB array contains operator
	 */
	public void arrayContains_gaussdb() {
		functionRegistry.register( "array_contains", new GaussDBArrayContainsOperatorFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_contains_nullable", new GaussDBArrayContainsOperatorFunction( true, typeConfiguration ) );
		functionRegistry.register( "array_includes", new ArrayIncludesOperatorFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_includes_nullable", new ArrayIncludesOperatorFunction( true, typeConfiguration ) );
	}

	/**
	 * Oracle array_contains() function
	 */
	public void arrayContains_oracle() {
		functionRegistry.register( "array_contains", new OracleArrayContainsFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_contains_nullable", new OracleArrayContainsFunction( true, typeConfiguration ) );
		functionRegistry.register( "array_includes", new OracleArrayIncludesFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_includes_nullable", new OracleArrayIncludesFunction( true, typeConfiguration ) );
	}

	/**
	 * H2 array_intersects() function
	 */
	public void arrayIntersects_h2(int maximumArraySize) {
		functionRegistry.register(
				"array_intersects",
				new H2ArrayIntersectsFunction( false, maximumArraySize, typeConfiguration )
		);
		functionRegistry.register(
				"array_intersects_nullable",
				new H2ArrayIntersectsFunction( true, maximumArraySize, typeConfiguration )
		);
		functionRegistry.registerAlternateKey( "array_overlaps", "array_intersects" );
		functionRegistry.registerAlternateKey( "array_overlaps_nullable", "array_intersects_nullable" );
	}

	/**
	 * HSQL array_intersects() function
	 */
	public void arrayIntersects_hsql() {
		functionRegistry.register(
				"array_intersects",
				new ArrayIntersectsUnnestFunction( false, typeConfiguration )
		);
		functionRegistry.register(
				"array_intersects_nullable",
				new ArrayIntersectsUnnestFunction( true, typeConfiguration )
		);
		functionRegistry.registerAlternateKey( "array_overlaps", "array_intersects" );
		functionRegistry.registerAlternateKey( "array_overlaps_nullable", "array_intersects_nullable" );
	}

	/**
	 * CockroachDB and PostgreSQL array intersects operator
	 */
	public void arrayIntersects_postgresql() {
		functionRegistry.register( "array_intersects", new ArrayIntersectsOperatorFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_intersects_nullable", new ArrayIntersectsOperatorFunction( true, typeConfiguration ) );
		functionRegistry.registerAlternateKey( "array_overlaps", "array_intersects" );
		functionRegistry.registerAlternateKey( "array_overlaps_nullable", "array_intersects_nullable" );
	}

	/**
	 * GaussDB array intersects operator
	 */
	public void arrayIntersects_gaussdb() {
		functionRegistry.register( "array_intersects", new ArrayIntersectsOperatorFunction( false, typeConfiguration ) );
		functionRegistry.register( "array_intersects_nullable", new ArrayIntersectsOperatorFunction( true, typeConfiguration ) );
		functionRegistry.registerAlternateKey( "array_overlaps", "array_intersects" );
		functionRegistry.registerAlternateKey( "array_overlaps_nullable", "array_intersects_nullable" );
	}

	/**
	 * Oracle array_intersects() function
	 */
	public void arrayIntersects_oracle() {
		functionRegistry.register(
				"array_intersects",
				new OracleArrayIntersectsFunction( typeConfiguration, false )
		);
		functionRegistry.register(
				"array_intersects_nullable",
				new OracleArrayIntersectsFunction( typeConfiguration, true )
		);
		functionRegistry.registerAlternateKey( "array_overlaps", "array_intersects" );
		functionRegistry.registerAlternateKey( "array_overlaps_nullable", "array_intersects_nullable" );
	}

	/**
	 * CockroachDB and PostgreSQL array_position() function
	 */
	public void arrayPosition_postgresql() {
		functionRegistry.register( "array_position", new PostgreSQLArrayPositionFunction( typeConfiguration ) );
	}

	/**
	 * GaussDB array_position() function
	 */
	public void arrayPosition_gaussdb() {
		functionRegistry.register( "array_position", new GaussDBArrayPositionFunction( typeConfiguration ) );
	}

	/**
	 * H2 array_position() function
	 */
	public void arrayPosition_h2(int maximumArraySize) {
		functionRegistry.register( "array_position", new H2ArrayPositionFunction( maximumArraySize, typeConfiguration ) );
	}

	/**
	 * HSQL array_position() function
	 */
	public void arrayPosition_hsql() {
		functionRegistry.register( "array_position", new HSQLArrayPositionFunction( typeConfiguration ) );
	}

	/**
	 * Oracle array_position() function
	 */
	public void arrayPosition_oracle() {
		functionRegistry.register( "array_position", new OracleArrayPositionFunction( typeConfiguration ) );
	}

	/**
	 * CockroachDB and PostgreSQL array_positions() function
	 */
	public void arrayPositions_postgresql() {
		functionRegistry.register(
				"array_positions",
				new PostgreSQLArrayPositionsFunction( false, typeConfiguration )
		);
		functionRegistry.register(
				"array_positions_list",
				new PostgreSQLArrayPositionsFunction( true, typeConfiguration )
		);
	}

	/**
	 * H2 array_positions() function
	 */
	public void arrayPositions_h2(int maximumArraySize) {
		functionRegistry.register(
				"array_positions",
				new H2ArrayPositionsFunction( false, maximumArraySize, typeConfiguration )
		);
		functionRegistry.register(
				"array_positions_list",
				new H2ArrayPositionsFunction( true, maximumArraySize, typeConfiguration )
		);
	}

	/**
	 * HSQL array_positions() function
	 */
	public void arrayPositions_hsql() {
		functionRegistry.register(
				"array_positions",
				new HSQLArrayPositionsFunction( false, typeConfiguration )
		);
		functionRegistry.register(
				"array_positions_list",
				new HSQLArrayPositionsFunction( true, typeConfiguration )
		);
	}

	/**
	 * Oracle array_positions() function
	 */
	public void arrayPositions_oracle() {
		functionRegistry.register(
				"array_positions",
				new OracleArrayPositionsFunction( false, typeConfiguration )
		);
		functionRegistry.register(
				"array_positions_list",
				new OracleArrayPositionsFunction( true, typeConfiguration )
		);
	}

	/**
	 * H2, HSQLDB, CockroachDB and PostgreSQL array_length() function
	 */
	public void arrayLength_cardinality() {
		functionRegistry.patternDescriptorBuilder( "array_length", "cardinality(?1)" )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant( integerType ) )
				.setArgumentsValidator(
						StandardArgumentsValidators.composite(
								StandardArgumentsValidators.exactly( 1 ),
								ArrayArgumentValidator.DEFAULT_INSTANCE
						)
				)
				.setArgumentListSignature( "(ARRAY array)" )
				.register();
		functionRegistry.register( "length", new DynamicDispatchFunction( functionRegistry, "character_length", "array_length" ) );
	}

	/**
	 * Oracle array_length() function
	 */
	public void arrayLength_oracle() {
		functionRegistry.register( "array_length", new OracleArrayLengthFunction( typeConfiguration ) );
		functionRegistry.register( "length", new DynamicDispatchFunction( functionRegistry, "character_length", "array_length" ) );
	}

	/**
	 * H2 and HSQLDB array_concat() function
	 */
	public void arrayConcat_operator() {
		functionRegistry.register( "array_concat", new ArrayConcatFunction( "", "||", "" ) );
	}

	/**
	 * CockroachDB and PostgreSQL array_concat() function
	 */
	public void arrayConcat_postgresql() {
		functionRegistry.register( "array_concat", new PostgreSQLArrayConcatFunction() );
	}

	/**
	 * PostgreSQL array_concat() function
	 */
	public void arrayConcat_gaussdb() {
		functionRegistry.register( "array_concat", new GaussDBArrayConcatFunction() );
	}

	/**
	 * Oracle array_concat() function
	 */
	public void arrayConcat_oracle() {
		functionRegistry.register( "array_concat", new OracleArrayConcatFunction() );
	}

	/**
	 * H2 and HSQLDB array_prepend() function
	 */
	public void arrayPrepend_operator() {
		functionRegistry.register( "array_prepend", new ArrayConcatElementFunction( "", "||", "", true ) );
	}

	/**
	 * CockroachDB and PostgreSQL array_prepend() function
	 */
	public void arrayPrepend_postgresql() {
		functionRegistry.register( "array_prepend", new PostgreSQLArrayConcatElementFunction( true ) );
	}

	/**
	 * GaussDB array_prepend() function
	 */
	public void arrayPrepend_gaussdb() {
		functionRegistry.register( "array_prepend", new GaussDBArrayConcatElementFunction( true ) );
	}

	/**
	 * Oracle array_prepend() function
	 */
	public void arrayPrepend_oracle() {
		functionRegistry.register( "array_prepend", new OracleArrayConcatElementFunction( true ) );
	}

	/**
	 * H2 and HSQLDB array_append() function
	 */
	public void arrayAppend_operator() {
		functionRegistry.register( "array_append", new ArrayConcatElementFunction( "", "||", "", false ) );
	}

	/**
	 * CockroachDB and PostgreSQL array_append() function
	 */
	public void arrayAppend_postgresql() {
		functionRegistry.register( "array_append", new PostgreSQLArrayConcatElementFunction( false ) );
	}

	/**
	 * GaussDB array_append() function
	 */
	public void arrayAppend_gaussdb() {
		functionRegistry.register( "array_append", new GaussDBArrayConcatElementFunction( false ) );
	}

	/**
	 * Oracle array_append() function
	 */
	public void arrayAppend_oracle() {
		functionRegistry.register( "array_append", new OracleArrayConcatElementFunction( false ) );
	}

	/**
	 * H2 array_get() function via bracket syntax
	 */
	public void arrayGet_h2() {
		functionRegistry.patternDescriptorBuilder( "array_get", "case when array_length(?1)>=?2 then ?1[?2] end" )
				.setReturnTypeResolver( ElementViaArrayArgumentReturnTypeResolver.DEFAULT_INSTANCE )
				.setArgumentsValidator(
						StandardArgumentsValidators.composite(
								ArrayArgumentValidator.DEFAULT_INSTANCE,
								new ArgumentTypesValidator( null, ANY, INTEGER )
						)
				)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER ) )
				.setArgumentListSignature( "(ARRAY array, INTEGER index)" )
				.register();
	}
	/**
	 * CockroachDB and PostgreSQL array_get() function via bracket syntax
	 */
	public void arrayGet_bracket() {
		functionRegistry.patternDescriptorBuilder( "array_get", "?1[?2]" )
				.setReturnTypeResolver( ElementViaArrayArgumentReturnTypeResolver.DEFAULT_INSTANCE )
				.setArgumentsValidator(
						StandardArgumentsValidators.composite(
								ArrayArgumentValidator.DEFAULT_INSTANCE,
								new ArgumentTypesValidator( null, ANY, INTEGER )
						)
				)
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER ) )
				.setArgumentListSignature( "(ARRAY array, INTEGER index)" )
				.register();
	}

	/**
	 * HSQL array_get() function
	 */
	public void arrayGet_unnest() {
		functionRegistry.register( "array_get", new ArrayGetUnnestFunction() );
	}

	/**
	 * Oracle array_get() function
	 */
	public void arrayGet_oracle() {
		functionRegistry.register( "array_get", new OracleArrayGetFunction() );
	}

	/**
	 * H2 array_set() function
	 */
	public void arraySet_h2(int maximumArraySize) {
		functionRegistry.register( "array_set", new H2ArraySetFunction( maximumArraySize ) );
	}

	/**
	 * HSQL array_set() function
	 */
	public void arraySet_hsql() {
		functionRegistry.register( "array_set", new HSQLArraySetFunction() );
	}

	/**
	 * CockroachDB and PostgreSQL array_set() function
	 */
	public void arraySet_unnest() {
		functionRegistry.register( "array_set", new ArraySetUnnestFunction() );
	}

	/**
	 * GaussDB array_set() function
	 */
	public void arraySet_gaussdb() {
		functionRegistry.register( "array_set", new GaussDBArraySetFunction() );
	}

	/**
	 * Oracle array_set() function
	 */
	public void arraySet_oracle() {
		functionRegistry.register( "array_set", new OracleArraySetFunction() );
	}

	/**
	 * CockroachDB and PostgreSQL array_remove() function
	 */
	public void arrayRemove() {
		functionRegistry.namedDescriptorBuilder( "array_remove" )
				.setArgumentsValidator(
						StandardArgumentsValidators.composite(
								StandardArgumentsValidators.exactly( 2 ),
								ArrayAndElementArgumentValidator.DEFAULT_INSTANCE
						)
				)
				.setReturnTypeResolver( ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE )
				.setArgumentTypeResolver( ArrayAndElementArgumentTypeResolver.DEFAULT_INSTANCE )
				.register();
	}

	/**
	 * H2 array_remove() function
	 */
	public void arrayRemove_h2(int maximumArraySize) {
		functionRegistry.register( "array_remove", new H2ArrayRemoveFunction( maximumArraySize ) );
	}

	/**
	 * GaussDB array_remove() function
	 */
	public void arrayRemove_gaussdb() {
		functionRegistry.register( "array_remove",  new GaussDBArrayRemoveFunction());
	}

	/**
	 * HSQL array_remove() function
	 */
	public void arrayRemove_hsql() {
		functionRegistry.register( "array_remove", new HSQLArrayRemoveFunction() );
	}

	/**
	 * Oracle array_remove() function
	 */
	public void arrayRemove_oracle() {
		functionRegistry.register( "array_remove", new OracleArrayRemoveFunction() );
	}

	/**
	 * GaussDB array_remove_index() function
	 */
	public void arrayRemoveIndex_gaussdb() {
		functionRegistry.register( "array_remove_index", new GaussDBArrayRemoveIndexFunction(false) );
	}

	/**
	 * H2 array_remove_index() function
	 */
	public void arrayRemoveIndex_h2(int maximumArraySize) {
		functionRegistry.register( "array_remove_index", new H2ArrayRemoveIndexFunction( maximumArraySize ) );
	}

	/**
	 * HSQL, CockroachDB and PostgreSQL array_remove_index() function
	 */
	public void arrayRemoveIndex_unnest(boolean castEmptyArrayLiteral) {
		functionRegistry.register( "array_remove_index", new ArrayRemoveIndexUnnestFunction( castEmptyArrayLiteral ) );
	}

	/**
	 * Oracle array_remove_index() function
	 */
	public void arrayRemoveIndex_oracle() {
		functionRegistry.register( "array_remove_index", new OracleArrayRemoveIndexFunction() );
	}

	/**
	 * H2 array_slice() function
	 */
	public void arraySlice() {
		functionRegistry.patternAggregateDescriptorBuilder( "array_slice", "case when ?1 is null or ?2 is null or ?3 is null then null else coalesce(array_slice(?1,?2,?3),array[]) end" )
				.setArgumentsValidator(
						StandardArgumentsValidators.composite(
								new ArgumentTypesValidator( null, ANY, INTEGER, INTEGER ),
								ArrayArgumentValidator.DEFAULT_INSTANCE
						)
				)
				.setReturnTypeResolver( ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE )
				.setArgumentTypeResolver(
						StandardFunctionArgumentTypeResolvers.composite(
								StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER, INTEGER ),
								StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
						)
				)
				.setArgumentListSignature( "(ARRAY array, INTEGER start, INTEGER end)" )
				.register();
	}

	/**
	 * HSQL array_slice() function
	 */
	public void arraySlice_unnest() {
		functionRegistry.register( "array_slice", new ArraySliceUnnestFunction( false ) );
	}

	/**
	 * CockroachDB and PostgreSQL array_slice() function
	 */
	public void arraySlice_operator() {
		functionRegistry.patternAggregateDescriptorBuilder( "array_slice", "?1[?2:?3]" )
				.setArgumentsValidator(
						StandardArgumentsValidators.composite(
								new ArgumentTypesValidator( null, ANY, INTEGER, INTEGER ),
								ArrayArgumentValidator.DEFAULT_INSTANCE
						)
				)
				.setReturnTypeResolver( ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE )
				.setArgumentTypeResolver(
						StandardFunctionArgumentTypeResolvers.composite(
								StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER, INTEGER ),
								StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
						)
				)
				.setArgumentListSignature( "(ARRAY array, INTEGER start, INTEGER end)" )
				.register();
	}

	/**
	 * Oracle array_slice() function
	 */
	public void arraySlice_oracle() {
		functionRegistry.register( "array_slice", new OracleArraySliceFunction() );
	}

	/**
	 * H2 array_replace() function
	 */
	public void arrayReplace_h2(int maximumArraySize) {
		functionRegistry.register( "array_replace", new H2ArrayReplaceFunction( maximumArraySize ) );
	}

	/**
	 * HSQL array_replace() function
	 */
	public void arrayReplace_unnest() {
		functionRegistry.register( "array_replace", new ArrayReplaceUnnestFunction() );
	}

	/**
	 * CockroachDB and PostgreSQL array_replace() function
	 */
	public void arrayReplace() {
		functionRegistry.namedDescriptorBuilder( "array_replace" )
				.setArgumentsValidator(
						StandardArgumentsValidators.composite(
								StandardArgumentsValidators.exactly( 3 ),
								new ArrayAndElementArgumentValidator( 0, 1, 2 )
						)
				)
				.setReturnTypeResolver( ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE )
				.setArgumentTypeResolver( new ArrayAndElementArgumentTypeResolver( 0, 1, 2 ) )
				.setArgumentListSignature( "(ARRAY array, OBJECT old, OBJECT new)" )
				.register();
	}

	/**
	 * Oracle array_replace() function
	 */
	public void arrayReplace_oracle() {
		functionRegistry.register( "array_replace", new OracleArrayReplaceFunction() );
	}

	/**
	 * GaussDB array_replace() function
	 */
	public void arrayReplace_gaussdb() {
		functionRegistry.register( "array_replace", new GaussDBArrayReplaceFunction() );
	}

	/**
	 * H2, HSQLDB, CockroachDB and PostgreSQL array_trim() function
	 */
	public void arrayTrim_trim_array() {
		functionRegistry.patternAggregateDescriptorBuilder( "array_trim", "trim_array(?1,?2)" )
				.setArgumentsValidator(
						StandardArgumentsValidators.composite(
								new ArgumentTypesValidator( null, ANY, INTEGER ),
								ArrayArgumentValidator.DEFAULT_INSTANCE
						)
				)
				.setReturnTypeResolver( ArrayViaArgumentReturnTypeResolver.DEFAULT_INSTANCE )
				.setArgumentTypeResolver(
						StandardFunctionArgumentTypeResolvers.composite(
								StandardFunctionArgumentTypeResolvers.invariant( ANY, INTEGER ),
								StandardFunctionArgumentTypeResolvers.IMPLIED_RESULT_TYPE
						)
				)
				.setArgumentListSignature( "(ARRAY array, INTEGER elementsToRemove)" )
				.register();
	}

	/**
	 * PostgreSQL array_trim() emulation for versions before 14
	 */
	public void arrayTrim_unnest() {
		functionRegistry.register( "array_trim", new PostgreSQLArrayTrimEmulation() );
	}

	/**
	 * Oracle array_trim() function
	 */
	public void arrayTrim_oracle() {
		functionRegistry.register( "array_trim", new OracleArrayTrimFunction() );
	}

	/**
	 * GaussDB array_trim() emulation for versions before 14
	 */
	public void arrayTrim_gaussdb() {
		functionRegistry.register( "array_trim", new GaussDBArrayTrimFunction() );
	}

	/**
	 * H2 array_fill() function
	 */
	public void arrayFill_h2() {
		functionRegistry.register( "array_fill", new H2ArrayFillFunction( false ) );
		functionRegistry.register( "array_fill_list", new H2ArrayFillFunction( true ) );
	}

	/**
	 * HSQLDB array_fill() function
	 */
	public void arrayFill_hsql() {
		functionRegistry.register( "array_fill", new HSQLArrayFillFunction( false ) );
		functionRegistry.register( "array_fill_list", new HSQLArrayFillFunction( true ) );
	}

	/**
	 * PostgreSQL array_fill() function
	 */
	public void arrayFill_postgresql() {
		functionRegistry.register( "array_fill", new PostgreSQLArrayFillFunction( false ) );
		functionRegistry.register( "array_fill_list", new PostgreSQLArrayFillFunction( true ) );
	}

	/**
	 * GaussDB array_fill() function
	 */
	public void arrayFill_gaussdb() {
		functionRegistry.register( "array_fill", new GaussDBArrayFillFunction( false ) );
		functionRegistry.register( "array_fill_list", new GaussDBArrayFillFunction( true ) );
	}

	/**
	 * Cockroach array_fill() function
	 */
	public void arrayFill_cockroachdb() {
		functionRegistry.register( "array_fill", new CockroachArrayFillFunction( false ) );
		functionRegistry.register( "array_fill_list", new CockroachArrayFillFunction( true ) );
	}

	/**
	 * Oracle array_fill() function
	 */
	public void arrayFill_oracle() {
		functionRegistry.register( "array_fill", new OracleArrayFillFunction( false ) );
		functionRegistry.register( "array_fill_list", new OracleArrayFillFunction( true ) );
	}

	/**
	 * H2 array_to_string() function
	 */
	public void arrayToString_h2(int maximumArraySize) {
		functionRegistry.register( "array_to_string", new H2ArrayToStringFunction( maximumArraySize, typeConfiguration ) );
	}

	/**
	 * HSQL array_to_string() function
	 */
	public void arrayToString_hsql() {
		functionRegistry.register( "array_to_string", new HSQLArrayToStringFunction( typeConfiguration ) );
	}

	/**
	 * CockroachDB and PostgreSQL array_to_string() function
	 */
	public void arrayToString_postgresql() {
		functionRegistry.register( "array_to_string", new ArrayToStringFunction( typeConfiguration ) );
	}

	/**
	 * Oracle array_to_string() function
	 */
	public void arrayToString_oracle() {
		functionRegistry.register( "array_to_string", new OracleArrayToStringFunction( typeConfiguration ) );
	}

	/**
	 * HANA json_value() function
	 */
	public void jsonValue_no_passing() {
		functionRegistry.register( "json_value", new HANAJsonValueFunction( typeConfiguration ) );
	}

	/**
	 * Oracle json_value() function
	 */
	public void jsonValue_oracle() {
		functionRegistry.register( "json_value", new OracleJsonValueFunction( typeConfiguration ) );
	}

	/**
	 * DB2 json_value() function
	 */
	public void jsonValue_db2() {
		functionRegistry.register( "json_value", new DB2JsonValueFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_value() function
	 */
	public void jsonValue_postgresql(boolean supportsStandard) {
		functionRegistry.register( "json_value", new PostgreSQLJsonValueFunction( supportsStandard, typeConfiguration ) );
	}

	/**
	 * GaussDB json_value() function
	 */
	public void jsonValue_gaussdb(boolean supportsStandard) {
		functionRegistry.register( "json_value", new GaussDBJsonValueFunction( typeConfiguration ) );
	}

	/**
	 * CockroachDB json_value() function
	 */
	public void jsonValue_cockroachdb() {
		functionRegistry.register( "json_value", new CockroachDBJsonValueFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_value() function
	 */
	public void jsonValue_mysql() {
		functionRegistry.register( "json_value", new MySQLJsonValueFunction( typeConfiguration ) );
	}

	/**
	 * MariaDB json_value() function
	 */
	public void jsonValue_mariadb() {
		functionRegistry.register( "json_value", new MariaDBJsonValueFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server json_value() function
	 */
	public void jsonValue_sqlserver() {
		functionRegistry.register( "json_value", new SQLServerJsonValueFunction( typeConfiguration ) );
	}

	/**
	 * H2 json_value() function
	 */
	public void jsonValue_h2() {
		functionRegistry.register( "json_value", new H2JsonValueFunction( typeConfiguration ) );
	}

	/**
	 * json_query() function
	 */
	public void jsonQuery() {
		functionRegistry.register( "json_query", new JsonQueryFunction( typeConfiguration, true, true ) );
	}

	/**
	 * GaussDB json_query() function
	 */
	public void jsonQuery_gaussdb() {
		functionRegistry.register( "json_query", new GaussdbJsonQueryFunction( typeConfiguration ) );
	}

	/**
	 * json_query() function
	 */
	public void jsonQuery_no_passing() {
		functionRegistry.register( "json_query", new JsonQueryFunction( typeConfiguration, true, false ) );
	}

	/**
	 * Oracle json_query() function
	 */
	public void jsonQuery_oracle() {
		functionRegistry.register( "json_query", new JsonQueryFunction( typeConfiguration, false, false ) );
	}

	/**
	 * PostgreSQL json_query() function
	 */
	public void jsonQuery_postgresql() {
		functionRegistry.register( "json_query", new PostgreSQLJsonQueryFunction( typeConfiguration ) );
	}

	/**
	 * CockroachDB json_query() function
	 */
	public void jsonQuery_cockroachdb() {
		functionRegistry.register( "json_query", new CockroachDBJsonQueryFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_query() function
	 */
	public void jsonQuery_mysql() {
		functionRegistry.register( "json_query", new MySQLJsonQueryFunction( typeConfiguration ) );
	}

	/**
	 * MariaDB json_query() function
	 */
	public void jsonQuery_mariadb() {
		functionRegistry.register( "json_query", new MariaDBJsonQueryFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server json_query() function
	 */
	public void jsonQuery_sqlserver() {
		functionRegistry.register( "json_query", new SQLServerJsonQueryFunction( typeConfiguration ) );
	}

	/**
	 * H2 json_query() function
	 */
	public void jsonQuery_h2() {
		functionRegistry.register( "json_query", new H2JsonQueryFunction( typeConfiguration ) );
	}

	/**
	 * json_exists() function
	 */
	public void jsonExists() {
		functionRegistry.register( "json_exists", new JsonExistsFunction( typeConfiguration, true, true ) );
	}

	/**
	 * json_exists() function
	 */
	public void jsonExists_gaussdb() {
		functionRegistry.register( "json_exists", new GaussdbJsonExistsFunction( typeConfiguration, false, false ) );
	}

	/**
	 * json_exists() function that doesn't support the passing clause
	 */
	public void jsonExists_no_passing() {
		functionRegistry.register( "json_exists", new JsonExistsFunction( typeConfiguration, true, false ) );
	}

	/**
	 * Oracle json_exists() function
	 */
	public void jsonExists_oracle() {
		functionRegistry.register( "json_exists", new JsonExistsFunction( typeConfiguration, false, true ) );
	}

	/**
	 * H2 json_exists() function
	 */
	public void jsonExists_h2() {
		functionRegistry.register( "json_exists", new H2JsonExistsFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server json_exists() function
	 */
	public void jsonExists_sqlserver(boolean supportsExtendedJson) {
		functionRegistry.register( "json_exists", new SQLServerJsonExistsFunction( supportsExtendedJson, typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_exists() function
	 */
	public void jsonExists_postgresql() {
		functionRegistry.register( "json_exists", new PostgreSQLJsonExistsFunction( typeConfiguration ) );
	}

	/**
	 * CockroachDB json_exists() function
	 */
	public void jsonExists_cockroachdb() {
		functionRegistry.register( "json_exists", new CockroachDBJsonExistsFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_exists() function
	 */
	public void jsonExists_mysql() {
		functionRegistry.register( "json_exists", new MySQLJsonExistsFunction( typeConfiguration ) );
	}

	/**
	 * SAP HANA json_exists() function
	 */
	public void jsonExists_hana() {
		functionRegistry.register( "json_exists", new HANAJsonExistsFunction( typeConfiguration ) );
	}

	/**
	 * json_object() function
	 */
	public void jsonObject() {
		functionRegistry.register( "json_object", new JsonObjectFunction( typeConfiguration, true ) );
	}

	/**
	 * DB2 json_object() function
	 */
	public void jsonObject_db2() {
		functionRegistry.register( "json_object", new DB2JsonObjectFunction( typeConfiguration ) );
	}

	/**
	 * Oracle json_object() function
	 */
	public void jsonObject_oracle(boolean colonSyntax) {
		functionRegistry.register( "json_object", new OracleJsonObjectFunction( colonSyntax, typeConfiguration ) );
	}

	/**
	 * SQL Server json_object() function
	 */
	public void jsonObject_sqlserver(boolean supportsExtendedJson) {
		functionRegistry.register( "json_object", new SQLServerJsonObjectFunction( supportsExtendedJson, typeConfiguration ) );
	}

	/**
	 * SAP HANA json_object() function
	 */
	public void jsonObject_hana() {
		functionRegistry.register( "json_object", new HANAJsonObjectFunction( typeConfiguration ) );
	}

	/**
	 * HSQLDB json_object() function
	 */
	public void jsonObject_hsqldb() {
		functionRegistry.register( "json_object", new HSQLJsonObjectFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_object() function
	 */
	public void jsonObject_mysql() {
		functionRegistry.register( "json_object", new MySQLJsonObjectFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_object() function
	 */
	public void jsonObject_postgresql() {
		functionRegistry.register( "json_object", new PostgreSQLJsonObjectFunction( typeConfiguration ) );
	}

	/**
	 * GaussDB json_object() function
	 */
	public void jsonObject_gaussdb() {
		functionRegistry.register( "json_object", new GaussDBJsonObjectFunction( typeConfiguration ) );
	}

	/**
	 * json_array() function
	 */
	public void jsonArray() {
		functionRegistry.register( "json_array", new JsonArrayFunction( typeConfiguration ) );
	}

	/**
	 * DB2 json_array() function
	 */
	public void jsonArray_db2() {
		functionRegistry.register( "json_array", new DB2JsonArrayFunction( typeConfiguration ) );
	}

	/**
	 * Oracle json_array() function
	 */
	public void jsonArray_oracle() {
		functionRegistry.register( "json_array", new OracleJsonArrayFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server json_array() function
	 */
	public void jsonArray_sqlserver(boolean supportsExtendedJson) {
		functionRegistry.register( "json_array", new SQLServerJsonArrayFunction( supportsExtendedJson, typeConfiguration ) );
	}

	/**
	 * GaussDB json_array() function
	 */
	public void jsonArray_gaussdb() {
		functionRegistry.register( "json_array", new GaussDBJsonArrayFunction( typeConfiguration ) );
	}

	/**
	 * SAP HANA json_array() function
	 */
	public void jsonArray_hana() {
		functionRegistry.register( "json_array", new HANAJsonArrayFunction( typeConfiguration ) );
	}

	/**
	 * HSQLDB json_array() function
	 */
	public void jsonArray_hsqldb() {
		functionRegistry.register( "json_array", new HSQLJsonArrayFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_array() function
	 */
	public void jsonArray_mysql() {
		functionRegistry.register( "json_array", new MySQLJsonArrayFunction( typeConfiguration ) );
	}

	/**
	 * MariaDB json_array() function
	 */
	public void jsonArray_mariadb() {
		functionRegistry.register( "json_array", new MariaDBJsonArrayFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_array() function
	 */
	public void jsonArray_postgresql() {
		functionRegistry.register( "json_array", new PostgreSQLJsonArrayFunction( typeConfiguration ) );
	}

	/**
	 * H2 json_arrayagg() function
	 */
	public void jsonArrayAgg_h2() {
		functionRegistry.register( "json_arrayagg", new H2JsonArrayAggFunction( typeConfiguration ) );
	}

	/**
	 * HSQLDB json_arrayagg() function
	 */
	public void jsonArrayAgg_hsqldb() {
		functionRegistry.register( "json_arrayagg", new HSQLJsonArrayAggFunction( typeConfiguration ) );
	}

	/**
	 * Oracle json_arrayagg() function
	 */
	public void jsonArrayAgg_oracle() {
		functionRegistry.register( "json_arrayagg", new OracleJsonArrayAggFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_arrayagg() function
	 */
	public void jsonArrayAgg_postgresql(boolean supportsStandard) {
		functionRegistry.register( "json_arrayagg", new PostgreSQLJsonArrayAggFunction( supportsStandard, typeConfiguration ) );
	}

	/**
	 * GaussDB json_arrayagg() function
	 */
	public void jsonArrayAgg_gaussdb(boolean supportsStandard) {
		functionRegistry.register( "json_arrayagg", new GaussDBJsonArrayAggFunction( supportsStandard, typeConfiguration ) );
	}

	/**
	 * SQL Server json_arrayagg() function
	 */
	public void jsonArrayAgg_sqlserver(boolean supportsExtendedJson) {
		functionRegistry.register( "json_arrayagg", new SQLServerJsonArrayAggFunction( supportsExtendedJson, typeConfiguration ) );
	}

	/**
	 * MySQL json_arrayagg() function
	 */
	public void jsonArrayAgg_mysql() {
		functionRegistry.register( "json_arrayagg", new MySQLJsonArrayAggFunction( typeConfiguration ) );
	}

	/**
	 * MariaDB json_arrayagg() function
	 */
	public void jsonArrayAgg_mariadb() {
		functionRegistry.register( "json_arrayagg", new MariaDBJsonArrayAggFunction( typeConfiguration ) );
	}

	/**
	 * DB2 json_arrayagg() function
	 */
	public void jsonArrayAgg_db2() {
		functionRegistry.register( "json_arrayagg", new DB2JsonArrayAggFunction( typeConfiguration ) );
	}

	/**
	 * HANA json_arrayagg() function
	 */
	public void jsonArrayAgg_hana() {
		functionRegistry.register( "json_arrayagg", new HANAJsonArrayAggFunction( typeConfiguration ) );
	}

	/**
	 * Oracle json_objectagg() function
	 */
	public void jsonObjectAgg_oracle() {
		functionRegistry.register( "json_objectagg", new OracleJsonObjectAggFunction( typeConfiguration ) );
	}

	/**
	 * json_objectagg() function for H2 and HSQLDB
	 */
	public void jsonObjectAgg_h2() {
		functionRegistry.register( "json_objectagg", new H2JsonObjectAggFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_objectagg() function
	 */
	public void jsonObjectAgg_postgresql(boolean supportsStandard) {
		functionRegistry.register( "json_objectagg", new PostgreSQLJsonObjectAggFunction( supportsStandard, typeConfiguration ) );
	}

	/**
	 * GaussDB json_objectagg() function
	 */
	public void jsonObjectAgg_gaussdb(boolean supportsStandard) {
		functionRegistry.register( "json_objectagg", new GaussDBJsonObjectAggFunction( supportsStandard, typeConfiguration ) );
	}

	/**
	 * MySQL json_objectagg() function
	 */
	public void jsonObjectAgg_mysql() {
		functionRegistry.register( "json_objectagg", new MySQLJsonObjectAggFunction( typeConfiguration ) );
	}

	/**
	 * MariaDB json_objectagg() function
	 */
	public void jsonObjectAgg_mariadb() {
		functionRegistry.register( "json_objectagg", new MariaDBJsonObjectAggFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server json_objectagg() function
	 */
	public void jsonObjectAgg_sqlserver(boolean supportsExtendedJson) {
		functionRegistry.register( "json_objectagg", new SQLServerJsonObjectAggFunction( supportsExtendedJson, typeConfiguration ) );
	}

	/**
	 * HANA json_objectagg() function
	 */
	public void jsonObjectAgg_hana() {
		functionRegistry.register( "json_objectagg", new HANAJsonObjectAggFunction( typeConfiguration ) );
	}

	/**
	 * DB2 json_objectagg() function
	 */
	public void jsonObjectAgg_db2() {
		functionRegistry.register( "json_objectagg", new DB2JsonObjectAggFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_set() function
	 */
	public void jsonSet_postgresql() {
		functionRegistry.register( "json_set", new PostgreSQLJsonSetFunction( typeConfiguration ) );
	}

	/**
	 * GaussDB json_set() function
	 */
	public void jsonSet_gaussdb() {
		functionRegistry.register( "json_set", new GaussDBJsonSetFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_set() function
	 */
	public void jsonSet_mysql() {
		functionRegistry.namedDescriptorBuilder( "json_set" )
				.setArgumentsValidator( new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.STRING,
						FunctionParameterType.ANY
				) )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				) )
				.register();
	}

	/**
	 * Oracle json_set() function
	 */
	public void jsonSet_oracle() {
		functionRegistry.register( "json_set", new OracleJsonSetFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server json_set() function
	 */
	public void jsonSet_sqlserver() {
		functionRegistry.register( "json_set", new SQLServerJsonSetFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_remove() function
	 */
	public void jsonRemove_postgresql() {
		functionRegistry.register( "json_remove", new PostgreSQLJsonRemoveFunction( typeConfiguration ) );
	}

	/**
	 * GaussDB json_remove() function
	 */
	public void jsonRemove_gaussdb() {
		functionRegistry.register( "json_remove", new GaussDBJsonRemoveFunction( typeConfiguration ) );
	}

	/**
	 * CockroachDB json_remove() function
	 */
	public void jsonRemove_cockroachdb() {
		functionRegistry.register( "json_remove", new CockroachDBJsonRemoveFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_remove() function
	 */
	public void jsonRemove_mysql() {
		functionRegistry.namedDescriptorBuilder( "json_remove" )
				.setArgumentsValidator( new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 2 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.STRING
				) )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				) )
				.register();
	}

	/**
	 * Oracle json_remove() function
	 */
	public void jsonRemove_oracle() {
		functionRegistry.register( "json_remove", new OracleJsonRemoveFunction( typeConfiguration ) );
	}

	/**
	 * SQL server json_remove() function
	 */
	public void jsonRemove_sqlserver() {
		functionRegistry.register( "json_remove", new SQLServerJsonRemoveFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_replace() function
	 */
	public void jsonReplace_postgresql() {
		functionRegistry.register( "json_replace", new PostgreSQLJsonReplaceFunction( typeConfiguration ) );
	}

	/**
	 * GaussDB json_replace() function
	 */
	public void jsonReplace_gaussdb() {
		functionRegistry.register( "json_replace", new GaussDBJsonReplaceFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_replace() function
	 */
	public void jsonReplace_mysql() {
		functionRegistry.namedDescriptorBuilder( "json_replace" )
				.setArgumentsValidator( new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.STRING,
						FunctionParameterType.ANY
				) )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				) )
				.register();
	}

	/**
	 * Oracle json_replace() function
	 */
	public void jsonReplace_oracle() {
		functionRegistry.register( "json_replace", new OracleJsonReplaceFunction( typeConfiguration ) );
	}

	/**
	 * SQL server json_replace() function
	 */
	public void jsonReplace_sqlserver(boolean supportsExtendedJson) {
		functionRegistry.register( "json_replace", new SQLServerJsonReplaceFunction( supportsExtendedJson, typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_insert() function
	 */
	public void jsonInsert_postgresql() {
		functionRegistry.register( "json_insert", new PostgreSQLJsonInsertFunction( typeConfiguration ) );
	}

	/**
	 * GaussDB json_insert() function
	 */
	public void jsonInsert_gaussdb() {
		functionRegistry.register( "json_insert", new GaussDBJsonInsertFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_insert() function
	 */
	public void jsonInsert_mysql() {
		functionRegistry.namedDescriptorBuilder( "json_insert" )
				.setArgumentsValidator( new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.STRING,
						FunctionParameterType.ANY
				) )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				) )
				.register();
	}

	/**
	 * Oracle json_insert() function
	 */
	public void jsonInsert_oracle() {
		functionRegistry.register( "json_insert", new OracleJsonInsertFunction( typeConfiguration ) );
	}

	/**
	 * SQL server json_insert() function
	 */
	public void jsonInsert_sqlserver(boolean supportsExtendedJson) {
		functionRegistry.register( "json_insert", new SQLServerJsonInsertFunction( supportsExtendedJson, typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_mergepatch() function
	 */
	public void jsonMergepatch_postgresql() {
		functionRegistry.register( "json_mergepatch", new PostgreSQLJsonMergepatchFunction( typeConfiguration ) );
	}

	/**
	 * GaussDB json_mergepatch() function
	 */
	public void jsonMergepatch_gaussdb() {
		functionRegistry.register( "json_mergepatch", new GaussDBJsonMergepatchFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_mergepatch() function
	 */
	public void jsonMergepatch_mysql() {
		functionRegistry.namedDescriptorBuilder( "json_mergepatch", "json_merge_patch" )
				.setArgumentsValidator( new ArgumentTypesValidator(
						StandardArgumentsValidators.min( 2 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.IMPLICIT_JSON
				) )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				) )
				.register();
	}

	/**
	 * Oracle json_mergepatch() function
	 */
	public void jsonMergepatch_oracle() {
		functionRegistry.register( "json_mergepatch", new OracleJsonMergepatchFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_array_append() function
	 */
	public void jsonArrayAppend_postgresql(boolean supportsLax) {
		functionRegistry.register( "json_array_append", new PostgreSQLJsonArrayAppendFunction( supportsLax, typeConfiguration ) );
	}

	/**
	 * GaussDB json_array_append() function
	 */
	public void jsonArrayAppend_gaussdb(boolean supportsLax) {
		functionRegistry.register( "json_array_append", new GaussDBJsonArrayAppendFunction( supportsLax, typeConfiguration ) );
	}

	/**
	 * MySQL json_array_append() function
	 */
	public void jsonArrayAppend_mysql() {
		functionRegistry.namedDescriptorBuilder( "json_array_append" )
				.setArgumentsValidator( new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.STRING,
						FunctionParameterType.ANY
				) )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				) )
				.register();
	}

	/**
	 * MariaDB json_array_append() function
	 */
	public void jsonArrayAppend_mariadb() {
		functionRegistry.register( "json_array_append", new MariaDBJsonArrayAppendFunction( typeConfiguration ) );
	}

	/**
	 * Oracle json_array_append() function
	 */
	public void jsonArrayAppend_oracle() {
		functionRegistry.register( "json_array_append", new OracleJsonArrayAppendFunction( typeConfiguration ) );
	}

	/**
	 * SQL server json_array_append() function
	 */
	public void jsonArrayAppend_sqlserver(boolean supportsExtendedJson) {
		functionRegistry.register( "json_array_append", new SQLServerJsonArrayAppendFunction( supportsExtendedJson, typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_array_insert() function
	 */
	public void jsonArrayInsert_postgresql() {
		functionRegistry.register( "json_array_insert", new PostgreSQLJsonArrayInsertFunction( typeConfiguration ) );
	}

	/**
	 * gauss json_array_insert() function
	 */
	public void jsonArrayInsert_gauss() {
		functionRegistry.register( "json_array_insert", new GaussDBJsonArrayInsertFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_array_insert() function
	 */
	public void jsonArrayInsert_mysql() {
		functionRegistry.namedDescriptorBuilder( "json_array_insert" )
				.setArgumentsValidator( new ArgumentTypesValidator(
						StandardArgumentsValidators.exactly( 3 ),
						FunctionParameterType.IMPLICIT_JSON,
						FunctionParameterType.STRING,
						FunctionParameterType.ANY
				) )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.JSON )
				) )
				.register();
	}

	/**
	 * Oracle json_array_insert() function
	 */
	public void jsonArrayInsert_oracle() {
		functionRegistry.register( "json_array_insert", new OracleJsonArrayInsertFunction( typeConfiguration ) );
	}

	/**
	 * SQL server json_array_insert() function
	 */
	public void jsonArrayInsert_sqlserver() {
		functionRegistry.register( "json_array_insert", new SQLServerJsonArrayInsertFunction( typeConfiguration ) );
	}

	/**
	 * Standard xmlelement() function
	 */
	public void xmlelement() {
		functionRegistry.register( "xmlelement", new XmlElementFunction( typeConfiguration ) );
	}

	/**
	 * H2 xmlelement() function
	 */
	public void xmlelement_h2() {
		functionRegistry.register( "xmlelement", new H2XmlElementFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server xmlelement() function
	 */
	public void xmlelement_sqlserver() {
		functionRegistry.register( "xmlelement", new SQLServerXmlElementFunction( typeConfiguration ) );
	}

	/**
	 * Standard xmlcomment() function
	 */
	public void xmlcomment() {
		functionRegistry.namedDescriptorBuilder( "xmlcomment" )
				.setExactArgumentCount( 1 )
				.setParameterTypes( STRING )
				.setInvariantType( typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.SQLXML ) )
				.register();
	}

	/**
	 * SQL Server xmlcomment() function
	 */
	public void xmlcomment_sqlserver() {
		functionRegistry.patternDescriptorBuilder( "xmlcomment", "cast(('<!--'+?1+'-->') AS xml)" )
				.setExactArgumentCount( 1 )
				.setParameterTypes( STRING )
				.setInvariantType( typeConfiguration.getBasicTypeRegistry().resolve( String.class, SqlTypes.SQLXML ) )
				.register();
	}

	/**
	 * Standard xmlforest() function
	 */
	public void xmlforest() {
		functionRegistry.register( "xmlforest", new XmlForestFunction( typeConfiguration ) );
	}

	/**
	 * H2 xmlforest() function
	 */
	public void xmlforest_h2() {
		functionRegistry.register( "xmlforest", new H2XmlForestFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server xmlforest() function
	 */
	public void xmlforest_sqlserver() {
		functionRegistry.register( "xmlforest", new SQLServerXmlForestFunction( typeConfiguration ) );
	}

	/**
	 * Standard xmlconcat() function
	 */
	public void xmlconcat() {
		functionRegistry.register( "xmlconcat", new XmlConcatFunction( typeConfiguration ) );
	}

	/**
	 * H2 xmlconcat() function
	 */
	public void xmlconcat_h2() {
		functionRegistry.register( "xmlconcat", new H2XmlConcatFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server xmlconcat() function
	 */
	public void xmlconcat_sqlserver() {
		functionRegistry.register( "xmlconcat", new SQLServerXmlConcatFunction( typeConfiguration ) );
	}

	/**
	 * Standard xmlpi() function
	 */
	public void xmlpi() {
		functionRegistry.register( "xmlpi", new XmlPiFunction( typeConfiguration ) );
	}

	/**
	 * H2 xmlpi() function
	 */
	public void xmlpi_h2() {
		functionRegistry.register( "xmlpi", new H2XmlPiFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server xmlpi() function
	 */
	public void xmlpi_sqlserver() {
		functionRegistry.register( "xmlpi", new SQLServerXmlPiFunction( typeConfiguration ) );
	}

	/**
	 * Oracle xmlquery() function
	 */
	public void xmlquery_oracle() {
		functionRegistry.register( "xmlquery", new XmlQueryFunction( true, typeConfiguration ) );
	}

	/**
	 * DB2 xmlquery() function
	 */
	public void xmlquery_db2() {
		functionRegistry.register( "xmlquery", new XmlQueryFunction( false, typeConfiguration ) );
	}

	/**
	 * DB2 10.5 xmlquery() function
	 */
	public void xmlquery_db2_legacy() {
		functionRegistry.register( "xmlquery", new LegacyDB2XmlQueryFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL xmlquery() function
	 */
	public void xmlquery_postgresql() {
		functionRegistry.register( "xmlquery", new PostgreSQLXmlQueryFunction( typeConfiguration ) );
	}

	/**
	 * GaussDB xmlquery() function
	 */
	public void xmlquery_gaussdb() {
		functionRegistry.register( "xmlquery", new GaussDBXmlQueryFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server xmlquery() function
	 */
	public void xmlquery_sqlserver() {
		functionRegistry.register( "xmlquery", new SQLServerXmlQueryFunction( typeConfiguration ) );
	}

	/**
	 * Standard xmlexists() function
	 */
	public void xmlexists() {
		functionRegistry.register( "xmlexists", new XmlExistsFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server xmlexists() function
	 */
	public void xmlexists_sqlserver() {
		functionRegistry.register( "xmlexists", new SQLServerXmlExistsFunction( typeConfiguration ) );
	}

	/**
	 * DB2 10.5 xmlexists() function
	 */
	public void xmlexists_db2_legacy() {
		functionRegistry.register( "xmlexists", new LegacyDB2XmlExistsFunction( typeConfiguration ) );
	}

	/**
	 * Standard xmlagg() function
	 */
	public void xmlagg() {
		functionRegistry.register( "xmlagg", new XmlAggFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server xmlagg() function
	 */
	public void xmlagg_sqlserver() {
		functionRegistry.register( "xmlagg", new SQLServerXmlAggFunction( typeConfiguration ) );
	}

	/**
	 * Standard unnest() function
	 */
	public void unnest(@Nullable String defaultBasicArrayElementColumnName, String defaultIndexSelectionExpression) {
		functionRegistry.register( "unnest", new UnnestFunction( defaultBasicArrayElementColumnName, defaultIndexSelectionExpression ) );
	}

	/**
	 * Standard unnest() function for databases that don't support arrays natively
	 */
	public void unnest_emulated() {
		// Pass an arbitrary value
		unnest( "v", "i" );
	}

	/**
	 * H2 unnest() function
	 */
	public void unnest_h2(int maxArraySize) {
		functionRegistry.register( "unnest", new H2UnnestFunction( maxArraySize ) );
	}

	/**
	 * Oracle unnest() function
	 */
	public void unnest_oracle() {
		functionRegistry.register( "unnest", new OracleUnnestFunction() );
	}

	/**
	 * PostgreSQL unnest() function
	 */
	public void unnest_postgresql(boolean supportsJsonTable) {
		functionRegistry.register( "unnest", new PostgreSQLUnnestFunction( supportsJsonTable ) );
	}

	/**
	 * SQL Server unnest() function
	 */
	public void unnest_sqlserver() {
		functionRegistry.register( "unnest", new SQLServerUnnestFunction() );
	}

	/**
	 * Sybase ASE unnest() function
	 */
	public void unnest_sybasease() {
		functionRegistry.register( "unnest", new SybaseASEUnnestFunction() );
	}

	/**
	 * HANA unnest() function
	 */
	public void unnest_hana() {
		functionRegistry.register( "unnest", new HANAUnnestFunction() );
	}

	/**
	 * DB2 unnest() function
	 */
	public void unnest_db2(int maximumArraySize) {
		functionRegistry.register( "unnest", new DB2UnnestFunction( maximumArraySize ) );
	}

	/**
	 * Standard generate_series() function
	 */
	public void generateSeries(@Nullable String defaultValueColumnName, String defaultIndexSelectionExpression, boolean coerceToTimestamp) {
		functionRegistry.register( "generate_series", new GenerateSeriesFunction( defaultValueColumnName, defaultIndexSelectionExpression, coerceToTimestamp, typeConfiguration ) );
	}

	/**
	 * Recursive CTE generate_series() function
	 */
	public void generateSeries_recursive(int maxSeriesSize, boolean supportsInterval, boolean coerceToTimestamp) {
		functionRegistry.register( "generate_series", new CteGenerateSeriesFunction( maxSeriesSize, supportsInterval, coerceToTimestamp, typeConfiguration ) );
	}

	/**
	 * H2 generate_series() function
	 */
	public void generateSeries_h2(int maxSeriesSize) {
		functionRegistry.register( "generate_series", new H2GenerateSeriesFunction( maxSeriesSize, typeConfiguration ) );
	}

	/**
	 * SQL Server generate_series() function
	 */
	public void generateSeries_sqlserver(int maxSeriesSize) {
		functionRegistry.register( "generate_series", new SQLServerGenerateSeriesFunction( maxSeriesSize, typeConfiguration ) );
	}

	/**
	 * Sybase ASE generate_series() function
	 */
	public void generateSeries_sybasease(int maxSeriesSize) {
		functionRegistry.register( "generate_series", new SybaseASEGenerateSeriesFunction( maxSeriesSize, typeConfiguration ) );
	}

	/**
	 * HANA generate_series() function
	 */
	public void generateSeries_hana(int maxSeriesSize) {
		functionRegistry.register( "generate_series", new HANAGenerateSeriesFunction( maxSeriesSize, typeConfiguration ) );
	}

	/**
	 * Standard json_table() function
	 */
	public void jsonTable() {
		functionRegistry.register( "json_table", new JsonTableFunction( typeConfiguration ) );
	}

	/**
	 * Oracle json_table() function
	 */
	public void jsonTable_oracle() {
		functionRegistry.register( "json_table", new OracleJsonTableFunction( typeConfiguration ) );
	}

	/**
	 * PostgreSQL json_table() function
	 */
	public void jsonTable_postgresql() {
		functionRegistry.register( "json_table", new PostgreSQLJsonTableFunction( typeConfiguration ) );
	}

	/**
	 * CockroachDB json_table() function
	 */
	public void jsonTable_cockroachdb() {
		functionRegistry.register( "json_table", new CockroachDBJsonTableFunction( typeConfiguration ) );
	}

	/**
	 * MySQL json_table() function
	 */
	public void jsonTable_mysql() {
		functionRegistry.register( "json_table", new MySQLJsonTableFunction( typeConfiguration ) );
	}

	/**
	 * DB2 json_table() function
	 */
	public void jsonTable_db2(int maximumSeriesSize) {
		functionRegistry.register( "json_table", new DB2JsonTableFunction( maximumSeriesSize, typeConfiguration ) );
	}

	/**
	 * HANA json_table() function
	 */
	public void jsonTable_hana() {
		functionRegistry.register( "json_table", new HANAJsonTableFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server json_table() function
	 */
	public void jsonTable_sqlserver() {
		functionRegistry.register( "json_table", new SQLServerJsonTableFunction( typeConfiguration ) );
	}

	/**
	 * H2 json_table() function
	 */
	public void jsonTable_h2(int maximumArraySize) {
		functionRegistry.register( "json_table", new H2JsonTableFunction( maximumArraySize, typeConfiguration ) );
	}

	/**
	 * Standard xmltable() function
	 */
	public void xmltable(boolean supportsParametersInDefault) {
		functionRegistry.register( "xmltable", new XmlTableFunction( supportsParametersInDefault, typeConfiguration ) );
	}

	/**
	 * Oracle xmltable() function
	 */
	public void xmltable_oracle() {
		functionRegistry.register( "xmltable", new OracleXmlTableFunction( typeConfiguration ) );
	}

	/**
	 * DB2 xmltable() function
	 */
	public void xmltable_db2() {
		functionRegistry.register( "xmltable", new DB2XmlTableFunction( typeConfiguration ) );
	}

	/**
	 * HANA xmltable() function
	 */
	public void xmltable_hana() {
		functionRegistry.register( "xmltable", new HANAXmlTableFunction( typeConfiguration ) );
	}

	/**
	 * SQL Server xmltable() function
	 */
	public void xmltable_sqlserver() {
		functionRegistry.register( "xmltable", new SQLServerXmlTableFunction( typeConfiguration ) );
	}

	/**
	 * Sybase ASE xmltable() function
	 */
	public void xmltable_sybasease() {
		functionRegistry.register( "xmltable", new SybaseASEXmlTableFunction( typeConfiguration ) );
	}
}
