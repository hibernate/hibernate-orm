/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.function.Supplier;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * A group common function template definitions.  Centralized for easier use from
 * Dialects
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class CommonFunctionFactory {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// trigonometric/geometric functions

	public static void cosh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "cosh" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void cot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "cot" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.register();
	}

	public static void degrees(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "degrees" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.register();
	}

	public static void log(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "log" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.register();
	}

	public static void ln_log(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ln", "log" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void log10(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "log10" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.register();
	}

	public static void log2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "log2" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void radians(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "radians" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.register();
	}

	public static void sinh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sinh" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.register();
	}

	public static void tanh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "tanh" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.register();
	}

	public static void moreHyperbolic(QueryEngine queryEngine) {
		final BasicType<Double> doubleType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "acosh" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "asinh" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "atanh" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basic math functions

	public static void trunc(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "trunc" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setArgumentListSignature( "(number[, places])" )
				.register();
	}

	public static void truncate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "truncate" )
				.setExactArgumentCount( 2 ) //some databases allow 1 arg but in these it's a synonym for trunc()
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setArgumentListSignature( "(number, places)" )
				.register();
	}

	/**
	 * SQL Server
	 */
	public static void truncate_round(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "truncate", "round(?1,?2,1)" )
				.setExactArgumentCount( 2 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setArgumentListSignature( "(number, places)" )
				.register();
	}

	/**
	 * Returns double between 0.0 and 1.0. First call may specify a seed value.
	 */
	public static void rand(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "rand" )
				.setArgumentCountBetween( 0, 1 )
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setArgumentListSignature( "([seed])" )
				.register();
	}

	public static void median(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "median" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void median_percentileCont(QueryEngine queryEngine, boolean over) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder(
						"median",
						"percentile_cont(0.5) within group (order by ?1)"
								+ ( over ? " over()" : "" )
				)
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * Warning: the semantics of this function are inconsistent between DBs.
	 *
	 * - On Postgres it means stdev_samp()
	 * - On Oracle, DB2, MySQL it means stdev_pop()
	 */
	public static void stddev(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "stddev" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * Warning: the semantics of this function are inconsistent between DBs.
	 *
	 * - On Postgres it means var_samp()
	 * - On Oracle, DB2, MySQL it means var_pop()
	 */
	public static void variance(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "variance" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void stddevPopSamp(QueryEngine queryEngine) {
		final BasicType<Double> doubleType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "stddev_pop" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "stddev_samp" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void varPopSamp(QueryEngine queryEngine) {
		final BasicType<Double> doubleType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "var_pop" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "var_samp" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void covarPopSamp(QueryEngine queryEngine) {
		final BasicType<Double> doubleType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "covar_pop" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "covar_samp" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void corr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "corr" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void regrLinearRegressionAggregates(QueryEngine queryEngine) {
		final BasicType<Double> doubleType = queryEngine.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE );

		Arrays.asList(
						"regr_avgx", "regr_avgy", "regr_count", "regr_intercept", "regr_r2",
						"regr_slope", "regr_sxx", "regr_sxy", "regr_syy"
				)
				.forEach(
						fnName -> queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( fnName )
								.setInvariantType( doubleType )
								.setExactArgumentCount( 2 )
								.register()
				);
	}

	/**
	 * DB2
	 */
	public static void stdevVarianceSamp(QueryEngine queryEngine) {
		final BasicType<Double> doubleType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "stddev_samp" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "variance_samp" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * SQL Server-style
	 */
	public static void stddevPopSamp_stdevp(QueryEngine queryEngine) {
		final BasicType<Double> doubleType = queryEngine.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE );

		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "stdev" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "stdevp" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "stddev_samp", "stdev" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "stddev_pop", "stdevp" );
	}

	/**
	 * SQL Server-style
	 */
	public static void varPopSamp_varp(QueryEngine queryEngine) {
		BasicType<Double> doubleType = queryEngine.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE );
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "var" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "varp" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "var_samp", "var" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "var_pop", "varp" );
	}

	public static void pi(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "pi" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// character functions

	public static void soundex(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "soundex" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.register();
	}

	public static void trim2(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ltrim" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 1, 2 )
				.setArgumentListSignature( "(string[, characters])" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "rtrim" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 1, 2 )
				.setArgumentListSignature( "(string[, characters])" )
				.register();
	}

	public static void trim1(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ltrim" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.setArgumentListSignature( "(string)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "rtrim" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.setArgumentListSignature( "(string)" )
				.register();
	}

	public static void pad(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "lpad" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature( "(string, length[, padding])" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "rpad" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature( "(string, length[, padding])" )
				.register();
	}

	/**
	 * In MySQL the third argument is required
	 */
	public static void pad_space(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"lpad(?1,?2,' ')",
				"lpad(?1,?2,?3)"
		).setArgumentListSignature( "(string, length[, padding])" );
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"rpad(?1,?2,' ')",
				"rpad(?1,?2,?3)"
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	/**
	 * Transact-SQL
	 */
	public static void pad_replicate(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"(space(?2-len(?1))+?1)",
				"(replicate(?3,?2-len(?1))+?1)"
		).setArgumentListSignature( "(string, length[, padding])" );
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"(?1+space(?2-len(?1)))",
				"(?1+replicate(?3,?2-len(?1)))"
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	public static void pad_repeat(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"(repeat(' ',?2-character_length(?1))||?1)",
				"(repeat(?3,?2-character_length(?1))||?1)"
		).setArgumentListSignature( "(string, length[, padding])" );
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"(?1||repeat(' ',?2-character_length(?1)))",
				"(?1||repeat(?3,?2-character_length(?1)))"
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	/**
	 * SAP DB
	 */
	public static void pad_fill(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				stringType,
				"lfill(?1,' ',?2)",
				"lfill(?1,?3,?2)"
		).setArgumentListSignature( "(string, length[, padding])" );
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				stringType,
				"rfill(?1,' ',?2)",
				"rfill(?1,?3,?2)"
		).setArgumentListSignature( "(string, length[, padding])" );
	}

	public static void reverse(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "reverse" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void space(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "space" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void repeat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "repeat" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(string, times)" )
				.register();
	}

	public static void leftRight(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "left" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(string, length)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "right" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(string, length)" )
				.register();
	}

	public static void leftRight_substr(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "left", "substr(?1,1,?2)" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(string, length)" )
				.register();
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "right", "substr(?1,-?2)" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(string, length)" )
				.register();
	}

	public static void leftRight_substrLength(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "left", "substr(?1,1,?2)" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(string, length)" )
				.register();
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "right", "substr(?1,length(?1)-?2+1)" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(string, length)" )
				.register();
	}

	public static void repeat_replicate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "replicate" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(string, times)" )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "repeat", "replicate" );
	}

	public static void md5(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "md5" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void initcap(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "initcap" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void instr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "instr" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setArgumentCountBetween( 2, 4 )
				.setArgumentListSignature( "(string, pattern[, start[, occurrence]])" )
				.register();
	}

	public static void substr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "substr" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature( "(string, start[, length])" )
				.register();
	}

	public static void translate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "translate" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 3 )
				.register();
	}

	public static void bitand(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bitand" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitor(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bitor" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitxor(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bitxor" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitnot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bitnot" )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * Binary bitwise operators, not aggregate functions!
	 */
	public static void bitandorxornot_bitAndOrXorNot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bit_and" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitand", "bit_and" );

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bit_or" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitor", "bit_or" );

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bit_xor" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitxor", "bit_xor" );

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bit_not" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitnot", "bit_not" );
	}

	/**
	 * Bitwise operators, not aggregate functions!
	 */
	public static void bitandorxornot_binAndOrXorNot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bin_and" )
				.setMinArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitand", "bin_and" );

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bin_or" )
				.setMinArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitor", "bin_or" );

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bin_xor" )
				.setMinArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitxor", "bin_xor" );

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bin_not" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitnot", "bin_not" );
	}

	/**
	 * Binary bitwise operators, not aggregate functions!
	 */
	public static void bitandorxornot_operator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "bitand", "(?1&?2)" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "bitor", "(?1|?2)" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "bitxor", "(?1^?2)" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "bitnot", "~?1" )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * These are aggregate functions taking one argument!
	 */
	public static void bitAndOr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "bit_and" )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "bit_or" )
				.setExactArgumentCount( 1 )
				.register();

		//MySQL has it but how is that even useful?
//		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_xor" )
//				.setExactArgumentCount( 1 )
//				.register();
	}

	/**
	 * These are aggregate functions taking one argument!
	 */
	public static void everyAny(QueryEngine queryEngine) {
		final BasicType<Boolean> booleanType = queryEngine.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.BOOLEAN );

		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "every" )
				.setExactArgumentCount( 1 )
				.setInvariantType( booleanType )
				.setArgumentListSignature( "(predicate)" )
				.register();

		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "any" )
				.setExactArgumentCount( 1 )
				.setInvariantType( booleanType )
				.setArgumentListSignature( "(predicate)" )
				.register();
	}

	/**
	 * These are aggregate functions taking one argument, for
	 * databases that can directly aggregate both boolean columns
	 * and predicates!
	 */
	public static void everyAny_boolAndOr(QueryEngine queryEngine) {
		final BasicType<Boolean> booleanType = queryEngine.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.BOOLEAN );

		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "bool_and" )
				.setExactArgumentCount( 1 )
				.setInvariantType( booleanType )
				.setArgumentListSignature( "(predicate)" )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "every", "bool_and" );

		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "bool_or" )
				.setExactArgumentCount( 1 )
				.setInvariantType( booleanType )
				.setArgumentListSignature( "(predicate)" )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "any", "bool_or" );
	}

	/**
	 * These are aggregate functions taking one argument,
	 * for databases that have to emulate the boolean
	 * aggregation functions using sum() and case.
	 */
	public static void everyAny_sumCase(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().register( "every", new EveryAnyEmulation( queryEngine.getTypeConfiguration(), true ) );
		queryEngine.getSqmFunctionRegistry().register( "any", new EveryAnyEmulation( queryEngine.getTypeConfiguration(), false ) );
	}

	/**
	 * These are aggregate functions taking one argument,
	 * for SQL Server.
	 */
	public static void everyAny_sumIif(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().register( "every", new SQLServerEveryAnyEmulation( queryEngine.getTypeConfiguration(), true ) );
		queryEngine.getSqmFunctionRegistry().register( "any", new SQLServerEveryAnyEmulation( queryEngine.getTypeConfiguration(), false ) );
	}


	/**
	 * These are aggregate functions taking one argument,
	 * for Oracle.
	 */
	public static void everyAny_sumCaseCase(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().register( "every", new CaseWhenEveryAnyEmulation( queryEngine.getTypeConfiguration(), true ) );
		queryEngine.getSqmFunctionRegistry().register( "any", new CaseWhenEveryAnyEmulation( queryEngine.getTypeConfiguration(), false ) );
	}

	public static void yearMonthDay(QueryEngine queryEngine) {
		final BasicType<Integer> integerType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "day" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "month" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "year" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void hourMinuteSecond(QueryEngine queryEngine) {
		final BasicType<Integer> integerType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "hour" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "minute" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "second" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "microsecond" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void dayofweekmonthyear(QueryEngine queryEngine) {
		final BasicType<Integer> integerType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "dayofweek" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "dayofmonth" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "day", "dayofmonth" );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "dayofyear" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void dayOfWeekMonthYear(QueryEngine queryEngine) {
		final BasicType<Integer> integerType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "day_of_week" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "day_of_month" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "day", "day_of_month" );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "day_of_year" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void daynameMonthname(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "monthname" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "dayname" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void weekQuarter(QueryEngine queryEngine) {
		final BasicType<Integer> integerType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "week" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "quarter" )
				.setExactArgumentCount( 1 )
				.setInvariantType( integerType )
				.register();
	}

	public static void lastDay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "last_day" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DATE )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void lastDay_eomonth(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "eomonth" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DATE )
				)
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "last_date", "eomonth" );
	}

	public static void ceiling_ceil(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ceil" )
				.setExactArgumentCount( 1 )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "ceiling", "ceil" );
	}

	public static void toCharNumberDateTimestamp(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		//argument counts are right for Oracle, TimesTen, and CUBRID
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "to_number" )
				//always 1 arg on HSQL and Cache, always 2 on Postgres
				.setArgumentCountBetween( 1, 3 )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE ) )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "to_char" )
				.setArgumentCountBetween( 1, 3 )
				//always 2 args on HSQL and Postgres
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.STRING ) )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "to_date" )
				//always 2 args on HSQL and Postgres
				.setArgumentCountBetween( 1, 3 )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DATE ) )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "to_timestamp" )
				//always 2 args on HSQL and Postgres
				.setArgumentCountBetween( 1, 3 )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP ) )
				.register();
	}

	public static void dateTimeTimestamp(QueryEngine queryEngine) {
		date( queryEngine );
		time( queryEngine );
		timestamp( queryEngine );
	}

	public static void timestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "timestamp" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType(
						queryEngine.getTypeConfiguration()
								.getBasicTypeRegistry()
								.resolve( StandardBasicTypes.TIMESTAMP )
				)
				.register();
	}

	public static void time(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "time" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.TIME )
				)
				.register();
	}

	public static void date(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "date" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DATE )
				)
				.register();
	}

	public static void utcDateTimeTimestamp(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "utc_date" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DATE ) )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "utc_time" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIME ) )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "utc_timestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP ) )
				.register();
	}

	public static void currentUtcdatetimetimestamp(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_utcdate" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DATE ) )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_utctime" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIME ) )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_utctimestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP ) )
				.register();
	}

	public static void week_weekofyear(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "weekofyear" )
				.setInvariantType(
					queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "week", "weekofyear" );
	}

	/**
	 * Almost every database
	 */
	public static void concat_pipeOperator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "concat", "(?1||?2...)" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
//				.setMinArgumentCount( 1 )
				.setArgumentListSignature( "(string0[, string1[, ...]])" )
				.register();
	}

	/**
	 * Transact SQL-style
	 */
	public static void concat_plusOperator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "concat", "(?1+?2...)" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
//				.setMinArgumentCount( 1 )
				.setArgumentListSignature( "(string0[, string1[, ...]])" )
				.register();
	}

	/**
	 * Oracle-style
	 */
	public static void rownumRowid(QueryEngine queryEngine) {
		final BasicType<Long> longType = queryEngine.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "rowid" )
				.setInvariantType( longType )
				.setUseParenthesesWhenNoArgs( false )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "rownum" )
				.setInvariantType( longType )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	/**
	 * H2/HSQL-style
	 */
	public static void rownum(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "rownum" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.LONG )
				)
				.setUseParenthesesWhenNoArgs( true ) //H2 and HSQL require the parens
				.register();
	}

	/**
	 * CUBRID
	 */
	public static void rownumInstOrderbyGroupbyNum(QueryEngine queryEngine) {
		final BasicType<Integer> integerType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "rownum" )
				.setInvariantType( integerType )
				.setUseParenthesesWhenNoArgs( false )
				.register();

		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "inst_num" )
				.setInvariantType( integerType )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "orderby_num" )
				.setInvariantType( integerType )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "groupby_num" )
				.setInvariantType( integerType )
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	/**
	 * MySQL/CUBRID
	 */
	public static void makedateMaketime(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "makedate" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DATE ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(year, dayofyear)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "maketime" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIME ))
				.setExactArgumentCount( 3 )
				.setArgumentListSignature( "(hour, min, sec)" )
				.register();
	}

	/**
	 * Postgres
	 */
	public static void makeDateTimeTimestamp(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		BasicType<Date> timestampType = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "make_date" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DATE ) )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "make_time" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIME ) )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "make_timestamp" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 6 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "make_timestamptz" )
				.setInvariantType( timestampType )
				.setArgumentCountBetween( 6, 7 )
				.register();
	}

	public static void sysdate(QueryEngine queryEngine) {
		// returns a local timestamp
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "sysdate" )
				.setInvariantType(
						queryEngine.getTypeConfiguration()
								.getBasicTypeRegistry()
								.resolve( StandardBasicTypes.TIMESTAMP )
				)
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	/**
	 * MySQL requires the parens in sysdate()
	 */
	public static void sysdateParens(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "sysdate" )
				.setInvariantType(
						queryEngine.getTypeConfiguration()
								.getBasicTypeRegistry()
								.resolve( StandardBasicTypes.TIMESTAMP )
				)
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	/**
	 * MySQL 5.7 precision defaults to seconds, but microseconds is better
	 */
	public static void sysdateExplicitMicros(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "sysdate", "sysdate(6)" )
				.setInvariantType(
						queryEngine.getTypeConfiguration()
								.getBasicTypeRegistry()
								.resolve( StandardBasicTypes.TIMESTAMP )
				)
				.setExactArgumentCount( 0 )
				.register();
	}

	public static void systimestamp(QueryEngine queryEngine) {
		// returns a timestamp with timezone
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "systimestamp" )
				.setInvariantType(
						queryEngine.getTypeConfiguration()
								.getBasicTypeRegistry()
								.resolve( StandardBasicTypes.TIMESTAMP )
				)
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	public static void localtimeLocaltimestamp(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		//these functions return times without timezones
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "localtime" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIME ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "localtimestamp" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();

		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "local_time", "localtime" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_TIME ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "local_datetime", "localtimestamp" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.LOCAL_DATE_TIME ) )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	public static void trigonometry(QueryEngine queryEngine) {
		final BasicType<Double> doubleType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.DOUBLE );

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sin" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "cos" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "tan" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "asin" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "acos" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "atan" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "atan2" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * Transact-SQL atan2 is misspelled
	 */
	public static void atan2_atn2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "atan2", "atn2" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void coalesce(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "coalesce" )
				.setMinArgumentCount( 1 )
				.register();
	}

	/**
	 * SAP DB
	 */
	public static void coalesce_value(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "value" )
				.setMinArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "coalesce", "value" );
	}

	public static void nullif(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "nullif" )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * ANSI SQL-style
	 */
	public static void length_characterLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "character_length" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "length", "character_length" );
	}

	/**
	 * Transact SQL-style
	 */
	public static void characterLength_len(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "len" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "character_length", "len" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "length", "len" );
	}

	/**
	 * Oracle-style
	 */
	public static void characterLength_length(QueryEngine queryEngine, SqlAstNodeRenderingMode argumentRenderingMode) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "length" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 1 )
				.setArgumentRenderingMode( argumentRenderingMode )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "character_length", "length" );
	}

	public static void octetLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "octet_length" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void bitLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bit_length" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void bitLength_pattern(QueryEngine queryEngine, String pattern) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "bit_length", pattern )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * ANSI-style
	 */
	public static void position(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "position", "position(?1 in ?2)" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(pattern in string)" )
				.register();
	}

	public static void locate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "locate" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature( "(pattern, string[, start])" )
				.register();
	}

	/**
	 * Transact SQL-style
	 */
	public static void locate_charindex(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "charindex" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature( "(pattern, string[, start])" )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "locate", "charindex" );
	}

	/**
	 * locate() in terms of ANSI position() and substring()
	 */
	public static void locate_positionSubstring(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
						"locate",
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ),
						"position(?1 in ?2)", "(position(?1 in substring(?2 from ?3))+?3)"
				)
				.setArgumentListSignature( "(pattern, string[, start])" );
	}
	/**
	 * ANSI-style substring
	 */
	public static void substringFromFor(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
						"substring",
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ),
						"substring(?1 from ?2)", "substring(?1 from ?2 for ?3)"
				)
				.setArgumentListSignature( "(string{ from|,} start[{ for|,} length])" );
	}

	/**
	 * Not the same as ANSI-style substring!
	 */
	public static void substring(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "substring" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature( "(string{ from|,} start[{ for|,} length])" )
				.register();
	}

	/**
	 * Transact SQL-style (3 required args)
	 */
	public static void substring_substringLen(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry()
				.registerBinaryTernaryPattern(
						"substring",
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ),
						"substring(?1,?2,len(?1)-?2+1)",
						"substring(?1,?2,?3)"
				)
				.setArgumentListSignature( "(string{ from|,} start[{ for|,} length])" );
	}

	/**
	 * Oracle, and many others
	 */
	public static void substring_substr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "substring", "substr" )
				.setArgumentListSignature( "(string{ from|,} start[{ for|,} length])" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setArgumentCountBetween( 2, 3 )
				.register();
	}

	public static void insert(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "insert" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 4 )
				.setArgumentListSignature( "(string, start, length, replacement)" )
				.register();
	}

	/**
	 * Postgres
	 */
	public static void insert_overlay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder(
						"insert",
						"overlay(?1 placing ?4 from ?2 for ?3)"
				)
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 4 )
				.setArgumentListSignature( "(string, start, length, replacement)" )
				.register();
	}

	/**
	 * ANSI SQL form, supported by Postgres, HSQL
	 */
	public static void overlay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerTernaryQuaternaryPattern(
						"overlay",
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ),
						"overlay(?1 placing ?2 from ?3)",
						"overlay(?1 placing ?2 from ?3 for ?4)"
				)
				.setArgumentListSignature( "(string placing replacement from start[ for length])" );
	}

	/**
	 * For DB2 which has a broken implementation of overlay()
	 */
	public static void overlayCharacterLength_overlay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerTernaryQuaternaryPattern(
						"overlay",
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING ),
						//use character_length() here instead of length()
						//because DB2 doesn't like "length(?)"
						"overlay(?1 placing ?2 from ?3 for character_length(?2))",
						"overlay(?1 placing ?2 from ?3 for ?4)"
				)
				.setArgumentListSignature( "(string placing replacement from start[ for length])" );
	}

	public static void replace(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "replace" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 3 )
				.setArgumentListSignature( "(string, pattern, replacement)" )
				.register();
	}

	/**
	 * Sybase
	 */
	public static void replace_strReplace(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "str_replace" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 3 )
				.setArgumentListSignature( "(string, pattern, replacement)" )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "replace", "str_replace" );
	}

	public static void concat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "concat" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setMinArgumentCount( 1 )
				.setArgumentListSignature( "(string0[, string1[, ...]])" )
				.register();
	}

	public static void lowerUpper(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "lower" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.setArgumentListSignature( "(string)" )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "upper" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.setArgumentListSignature( "(string)" )
				.register();
	}

	public static void ascii(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ascii" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)//should it be BYTE??
				.register();
	}

	public static void char_chr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "chr" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.CHARACTER )
				)
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "char", "chr" );
	}

	public static void chr_char(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "char" )
				.setExactArgumentCount( 1 )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.CHARACTER )
				)
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "chr", "char" );
	}

	/**
	 * Transact SQL-style
	 */
	public static void datepartDatename(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datepart" )
//				.setInvariantType( StandardBasicTypes.INTEGER )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(field, arg)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datename" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(field, arg)" )
				.register();
	}

	// No real consistency in the semantics of these functions:
	// H2, HSQL: now()/curtime()/curdate() mean localtimestamp/localtime/current_date
	// MySQL, Cache: now()/curtime()/curdate() mean current_timestamp/current_time/current_date
	// CUBRID: now()/curtime()/curdate() mean current_datetime/current_time/current_date
	// Postgres: now() means current_timestamp
	public static void nowCurdateCurtime(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "curtime" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIME ) )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "curdate" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.DATE ) )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "now" )
				.setInvariantType( basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP ) )
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	public static void leastGreatest(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "least" )
				.setMinArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "greatest" )
				.setMinArgumentCount( 2 )
				.register();
	}

	public static void leastGreatest_minMax(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "least", "min" )
				.setMinArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "greatest", "max" )
				.setMinArgumentCount( 2 )
				.register();
	}

	public static void leastGreatest_minMaxValue(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "least", "minvalue" )
				.setMinArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "greatest", "maxvalue" )
				.setMinArgumentCount( 2 )
				.register();
	}

	public static void aggregates(
			Dialect dialect,
			QueryEngine queryEngine,
			SqlAstNodeRenderingMode inferenceArgumentRenderingMode) {
		aggregates( dialect, queryEngine, inferenceArgumentRenderingMode, "||", null );
	}

	public static void aggregates(
			Dialect dialect,
			QueryEngine queryEngine,
			SqlAstNodeRenderingMode inferenceArgumentRenderingMode,
			String concatOperator,
			String concatArgumentCastType) {
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "max" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "min" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setExactArgumentCount( 1 )
				.register();

		final TypeConfiguration typeConfiguration = queryEngine.getTypeConfiguration();
		final BasicType<Long> longType = typeConfiguration.getBasicTypeForJavaType( Long.class );
		final BasicType<Double> doubleType = typeConfiguration.getBasicTypeForJavaType( Double.class );
		final BasicType<BigInteger> bigIntegerType = typeConfiguration.getBasicTypeForJavaType( BigInteger.class );
		final BasicType<BigDecimal> bigDecimalType = typeConfiguration.getBasicTypeForJavaType( BigDecimal.class );
		// Resolve according to JPA spec 4.8.5
		// SUM returns Long when applied to state fields of integral types (other than BigInteger);
		// Double when applied to state fields of floating point types;
		// BigInteger when applied to state fields of type BigInteger;
		// and BigDecimal when applied to state fields of type BigDecimal.
		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "sum" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setReturnTypeResolver(
						new FunctionReturnTypeResolver() {
							@Override
							public AllowableFunctionReturnType<?> resolveFunctionReturnType(
									AllowableFunctionReturnType<?> impliedType,
									List<? extends SqmTypedNode<?>> arguments,
									TypeConfiguration typeConfiguration) {
								if ( impliedType != null ) {
									return impliedType;
								}
								final AllowableFunctionReturnType<?> argType = StandardFunctionReturnTypeResolvers.extractArgumentType(
										arguments,
										1
								);
								final BasicType<?> basicType;
								if ( argType instanceof BasicType<?> ) {
									basicType = (BasicType<?>) argType;
								}
								else {
									basicType = typeConfiguration.getBasicTypeForJavaType( argType.getJavaType() );
									if ( basicType == null ) {
										return impliedType;
									}
								}
								switch ( basicType.getJdbcTypeDescriptor().getJdbcTypeCode() ) {
									case Types.SMALLINT:
									case Types.TINYINT:
									case Types.INTEGER:
									case Types.BIGINT:
										return longType;
									case Types.FLOAT:
									case Types.REAL:
									case Types.DOUBLE:
										return doubleType;
									case Types.DECIMAL:
									case Types.NUMERIC:
										if ( BigInteger.class.isAssignableFrom( basicType.getJavaType() ) ) {
											return bigIntegerType;
										}
										else {
											return bigDecimalType;
										}
								}
								return bigDecimalType;
							}

							@Override
							public BasicValuedMapping resolveFunctionReturnType(
									Supplier<BasicValuedMapping> impliedTypeAccess,
									List<? extends SqlAstNode> arguments) {
								if ( impliedTypeAccess != null ) {
									final BasicValuedMapping basicValuedMapping = impliedTypeAccess.get();
									if ( basicValuedMapping != null ) {
										return basicValuedMapping;
									}
								}
								// Resolve according to JPA spec 4.8.5
								final BasicValuedMapping specifiedArgType = StandardFunctionReturnTypeResolvers.extractArgumentValuedMapping(
										arguments,
										1
								);
								switch ( specifiedArgType.getJdbcMapping().getJdbcTypeDescriptor().getJdbcTypeCode() ) {
									case Types.SMALLINT:
									case Types.TINYINT:
									case Types.INTEGER:
									case Types.BIGINT:
										return longType;
									case Types.FLOAT:
									case Types.REAL:
									case Types.DOUBLE:
										return doubleType;
									case Types.DECIMAL:
									case Types.NUMERIC:
										final Class<?> argTypeClass = specifiedArgType.getJdbcMapping()
												.getJavaTypeDescriptor()
												.getJavaTypeClass();
										if ( BigInteger.class.isAssignableFrom( argTypeClass ) ) {
											return bigIntegerType;
										}
										else {
											return bigDecimalType;
										}
								}
								return bigDecimalType;
							}

						}
				)
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedAggregateDescriptorBuilder( "avg" )
				.setArgumentRenderingMode( inferenceArgumentRenderingMode )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().register(
				CountFunction.FUNCTION_NAME,
				new CountFunction( dialect, queryEngine.getTypeConfiguration(), concatOperator, concatArgumentCastType )
		);
	}

	public static void math(QueryEngine queryEngine) {
		final BasicType<Integer> integerType = queryEngine.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "round" )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "floor" )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ceiling" )
				// To avoid truncating to a specific data type, we default to using the argument type
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "mod" )
				// According to JPA spec 4.6.17.2.2.
				.setInvariantType( integerType )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "abs" )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sign" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sqrt" )
				// According to JPA spec 4.6.17.2.2.
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ln" )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "exp" )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "power" )
				.setExactArgumentCount( 2 )
				.setReturnTypeResolver( new PowerReturnTypeResolver( queryEngine.getTypeConfiguration() ) )
				.register();
	}

	public static void mod_operator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "mod", "(?1%?2)" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void power_expLn(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "power", "exp(ln(?1)*?2)" )
				.setExactArgumentCount( 2 )
				.setReturnTypeResolver( new PowerReturnTypeResolver( queryEngine.getTypeConfiguration() ) )
				.register();
	}

	public static void square(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "square" )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void cbrt(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "cbrt" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void crc32(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "crc32" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void sha1(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sha1" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void sha2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sha2" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void sha(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sha" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * MySQL style, returns the number of days between two dates
	 */
	public static void datediff(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datediff" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
	}

	/**
	 * MySQL style
	 */
	public static void adddateSubdateAddtimeSubtime(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "adddate" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, days)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "subdate" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, days)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "addtime" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, time)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "subtime" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, time)" )
				.register();
	}

	public static void addMonths(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_months" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setArgumentListSignature( "(datetime, months)" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void monthsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "months_between" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
	}

	public static void daysBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "days_between" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
	}

	public static void secondsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "seconds_between" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.LONG )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
	}

	public static void yearsMonthsDaysHoursMinutesSecondsBetween(QueryEngine queryEngine) {
		final BasicTypeRegistry basicTypeRegistry = queryEngine.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<Long> longType = basicTypeRegistry.resolve( StandardBasicTypes.LONG );
		final BasicType<Integer> integerType = basicTypeRegistry.resolve( StandardBasicTypes.INTEGER );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "years_between" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "months_between" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "days_between" )
				.setInvariantType( integerType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "hours_between" )
				.setInvariantType( longType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "minutes_between" )
				.setInvariantType( longType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "seconds_between" )
				.setInvariantType( longType )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(end, start)" )
				.register();
	}

	public static void addYearsMonthsDaysHoursMinutesSeconds(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_years" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, years)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_months" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, months)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_days" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, days)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_hours" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, hours)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_minutes" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, minutes)" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_seconds" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime, seconds)" )
				.register();
	}

	/**
	 * H2-style (uses Java's SimpleDateFormat directly so no need to translate format)
	 */
	public static void format_formatdatetime(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "format", "formatdatetime" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime as pattern)" )
				.register();
	}

	/**
	 * Usually Oracle-style (except for Informix which quite close to MySQL-style)
	 *
	 * @see org.hibernate.dialect.OracleDialect#datetimeFormat
	 */
	public static void format_toChar(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "format", "to_char" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime as pattern)" )
				.register();
	}

	/**
	 * MySQL-style (also Ingres)
	 *
	 * @see org.hibernate.dialect.MySQLDialect#datetimeFormat
	 */
	public static void format_dateFormat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "format", "date_format" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime as pattern)" )
				.register();
	}

	/**
	 * HANA's name for to_char() is still Oracle-style
	 *
	 *  @see org.hibernate.dialect.OracleDialect#datetimeFormat
	 */
	public static void format_toVarchar(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "format", "to_varchar" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(datetime as pattern)" )
				.register();
	}

	public static void dateTrunc(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "date_trunc", "date_trunc('?1',?2)" )
				.setInvariantType(
						queryEngine.getTypeConfiguration()
								.getBasicTypeRegistry()
								.resolve( StandardBasicTypes.TIMESTAMP )
				)
				.setExactArgumentCount( 2 )
				.setArgumentListSignature( "(field, datetime)" )
				.register();
	}

	private static class PowerReturnTypeResolver implements FunctionReturnTypeResolver {

		private final BasicType<Double> doubleType;

		private PowerReturnTypeResolver(TypeConfiguration typeConfiguration) {
			this.doubleType = typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE );
		}

		@Override
		public AllowableFunctionReturnType<?> resolveFunctionReturnType(
				AllowableFunctionReturnType<?> impliedType,
				List<? extends SqmTypedNode<?>> arguments,
				TypeConfiguration typeConfiguration) {
			final JdbcMapping baseType = StandardFunctionReturnTypeResolvers
					.extractArgumentJdbcMapping( typeConfiguration, arguments, 1 );
			final JdbcMapping powerType = StandardFunctionReturnTypeResolvers
					.extractArgumentJdbcMapping( typeConfiguration, arguments, 2 );

			if ( baseType.getJdbcTypeDescriptor().isDecimal() ) {
				return (AllowableFunctionReturnType<?>) arguments.get( 0 ).getNodeType();
			}
			else if ( powerType.getJdbcTypeDescriptor().isDecimal() ) {
				return (AllowableFunctionReturnType<?>) arguments.get( 1 ).getNodeType();
			}
			return typeConfiguration.getBasicTypeForJavaType( Double.class );
		}

		@Override
		public BasicValuedMapping resolveFunctionReturnType(
				Supplier<BasicValuedMapping> impliedTypeAccess, List<? extends SqlAstNode> arguments) {
			final BasicValuedMapping baseMapping = StandardFunctionReturnTypeResolvers.extractArgumentValuedMapping(
					arguments,
					1
			);
			final BasicValuedMapping powerMapping = StandardFunctionReturnTypeResolvers.extractArgumentValuedMapping(
					arguments,
					2
			);
			if ( baseMapping.getJdbcMapping().getJdbcTypeDescriptor().isDecimal() ) {
				return baseMapping;
			}
			else if ( powerMapping.getJdbcMapping().getJdbcTypeDescriptor().isDecimal() ) {
				return powerMapping;
			}
			return doubleType;
		}
	}
}
