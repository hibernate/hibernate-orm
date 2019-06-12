/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.spi.StandardSpiBasicTypes;

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
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "cosh" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void cot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "cot" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void degrees(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "degrees" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void log(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "log" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void ln_log(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ln", "log" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void log10(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "log10" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void log2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "log2" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void radians(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "radians" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void sinh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sinh" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void tanh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "tanh" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void moreHyperbolic(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "acosh" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "asinh" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "atanh" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basic math functions

	public static void trunc(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "trunc" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setArgumentListSignature("(number[, places])")
				.register();
	}

	public static void truncate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "truncate" )
				.setExactArgumentCount( 2 ) //some databases allow 1 arg but in these it's a synonym for trunc()
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setArgumentListSignature("(number, places)")
				.register();
	}

	/**
	 * SQL Server
	 */
	public static void truncate_round(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "truncate", "round(?1,?2,1)" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setArgumentListSignature("(number, places)")
				.register();
	}

	/**
	 * Returns double between 0.0 and 1.0. First call may specify a seed value.
	 */
	public static void rand(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "rand" )
				.setArgumentCountBetween( 0, 1 )
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setArgumentListSignature("([seed])")
				.register();
	}

	public static void median(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "median" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void median_percentileCont(QueryEngine queryEngine, boolean over) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "median",
				"percentile_cont(0.5) within group (order by ?1)"
						+ (over ? " over()" : "") )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
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
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "stddev" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
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
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "variance" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void stddevPopSamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "stddev_pop" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "stddev_samp" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void varPopSamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "var_pop" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "var_samp" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * DB2
	 */
	public static void stdevVarianceSamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "stddev_samp" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "variance_samp" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * SQL Server-style
	 */
	public static void stddevPopSamp_stdevp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "stdev" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "stdevp" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "stddev_samp", "stdev" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "stddev_pop", "stdevp" );
	}

	/**
	 * SQL Server-style
	 */
	public static void varPopSamp_varp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "var" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "varp" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "var_samp", "var" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "var_pop", "varp" );
	}

	public static void pi(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "pi" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// character functions

	public static void soundex(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "soundex" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();
	}

	public static void trim2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ltrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.setArgumentListSignature("(string[, characters])")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "rtrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.setArgumentListSignature("(string[, characters])")
				.register();
	}

	public static void trim1(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ltrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.setArgumentListSignature("(string)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "rtrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.setArgumentListSignature("(string)")
				.register();
	}

	public static void pad(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "lpad" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature("(string, length[, padding])")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "rpad" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature("(string, length[, padding])")
				.register();
	}

	/**
	 * In MySQL the third argument is required
	 */
	public static void pad_space(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				StandardSpiBasicTypes.STRING,
				"lpad(?1,?2,' ')",
				"lpad(?1,?2,?3)"
		).setArgumentListSignature("(string, length[, padding])");
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				StandardSpiBasicTypes.STRING,
				"rpad(?1,?2,' ')",
				"rpad(?1,?2,?3)"
		).setArgumentListSignature("(string, length[, padding])");
	}

	/**
	 * Transact-SQL
	 */
	public static void pad_replicate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				StandardSpiBasicTypes.STRING,
				"(space(?2-len(?1))+?1)",
				"(replicate(?3,?2-len(?1))+?1)"
		).setArgumentListSignature("(string, length[, padding])");
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				StandardSpiBasicTypes.STRING,
				"(?1+space(?2-len(?1)))",
				"(?1+replicate(?3,?2-len(?1)))"
		).setArgumentListSignature("(string, length[, padding])");
	}

	public static void pad_repeat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				StandardSpiBasicTypes.STRING,
				"(repeat(' ',?2-character_length(?1))||?1)",
				"(repeat(?3,?2-character_length(?1))||?1)"
		).setArgumentListSignature("(string, length[, padding])");
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				StandardSpiBasicTypes.STRING,
				"(?1||repeat(' ',?2-character_length(?1)))",
				"(?1||repeat(?3,?2-character_length(?1)))"
		).setArgumentListSignature("(string, length[, padding])");
	}

	/**
	 * SAP DB
	 */
	public static void pad_fill(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				StandardSpiBasicTypes.STRING,
				"lfill(?1,' ',?2)",
				"lfill(?1,?3,?2)"
		).setArgumentListSignature("(string, length[, padding])");
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				StandardSpiBasicTypes.STRING,
				"rfill(?1,' ',?2)",
				"rfill(?1,?3,?2)"
		).setArgumentListSignature("(string, length[, padding])");
	}

	public static void reverse(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "reverse" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void space(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "space" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void repeat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "repeat" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, times)")
				.register();
	}

	public static void leftRight(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "left" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, length)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "right" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, length)")
				.register();
	}

	public static void leftRight_substr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "left", "substr(?1,1,?2)" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, length)")
				.register();
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "right", "substr(?1,-?2)" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, length)")
				.register();
	}

	public static void leftRight_substrLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "left", "substr(?1,1,?2)" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, length)")
				.register();
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "right", "substr(?1,length(?1)-?2+1)" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, length)")
				.register();
	}

	public static void repeat_replicate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "replicate" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(string, times)")
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "repeat", "replicate" );
	}

	public static void md5(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "md5" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void initcap(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "initcap" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void instr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "instr" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 4 )
				.setArgumentListSignature("(string, pattern[, start[, occurrence]])")
				.register();
	}

	public static void substr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "substr" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature("(string, start[, length])")
				.register();
	}

	public static void translate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "translate" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 3 )
				.register();
	}

	public static void bitand(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bitand" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitor(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bitor" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitxor(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bitxor" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitnot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bitnot" )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * Binary bitwise operators, not aggregate functions!
	 */
	public static void bitandorxornot_bitAndOrXorNot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_and" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitand", "bit_and");

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_or" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitor", "bit_or");

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_xor" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitxor", "bit_xor");

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_not" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "bitnot", "bit_not");
	}

	/**
	 * Binary bitwise operators, not aggregate functions!
	 */
	public static void bitandorxornot_operator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "bitand", "(?1&?2)" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "bitor", "(?1|?2)" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "bitxor", "(?1^?2)" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "bitnot", "~?1" )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * These are aggregate functions taking one argument!
	 */
	public static void bitAndOr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_and" )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bit_or" )
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
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "every" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setArgumentListSignature("(predicate)")
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "any" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setArgumentListSignature("(predicate)")
				.register();
	}

	/**
	 * These are aggregate functions taking one argument, for
	 * databases that can directly aggregate both boolean columns
	 * and predicates!
	 */
	public static void everyAny_boolAndOr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bool_and" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setArgumentListSignature("(predicate)")
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "every", "bool_and" );

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bool_or" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setArgumentListSignature("(predicate)")
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "any", "bool_or" );
	}

	/**
	 * These are aggregate functions taking one argument,
	 * for databases that have to emulate the boolean
	 * aggregation functions using sum() and case.
	 */
	public static void everyAny_sumCase(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "every",
				"(sum(case when ?1 then 0 else 1 end)=0)" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setArgumentListSignature("(predicate)")
				.register();

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "any",
				"(sum(case when ?1 then 1 else 0 end)>0)" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setArgumentListSignature("(predicate)")
				.register();
	}

	public static void yearMonthDay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "day" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "month" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "year" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void hourMinuteSecond(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "hour" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "minute" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "second" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "microsecond" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void dayofweekmonthyear(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("dayofweek")
				.setInvariantType(StandardSpiBasicTypes.INTEGER)
				.setExactArgumentCount(1)
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("dayofmonth")
				.setInvariantType(StandardSpiBasicTypes.INTEGER)
				.setExactArgumentCount(1)
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey("day", "dayofmonth");
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("dayofyear")
				.setInvariantType(StandardSpiBasicTypes.INTEGER)
				.setExactArgumentCount(1)
				.register();
	}

	public static void dayOfWeekMonthYear(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("day_of_week")
				.setInvariantType(StandardSpiBasicTypes.INTEGER)
				.setExactArgumentCount(1)
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("day_of_month")
				.setInvariantType(StandardSpiBasicTypes.INTEGER)
				.setExactArgumentCount(1)
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey("day", "day_of_month");
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("day_of_year")
				.setInvariantType(StandardSpiBasicTypes.INTEGER)
				.setExactArgumentCount(1)
				.register();
	}

	public static void daynameMonthname(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "monthname" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "dayname" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void weekQuarter(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "week" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "quarter" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.register();
	}

	public static void lastDay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "last_day" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void lastDay_eomonth(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "eomonth" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "last_date", "eomonth" );
	}

	public static void ceiling_ceil(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ceil" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "ceiling", "ceil" );
	}

	public static void toCharNumberDateTimestamp(QueryEngine queryEngine) {
		//argument counts are right for Oracle, TimesTen, and CUBRID
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_number" )
				//always 1 arg on HSQL and Cache, always 2 on Postgres
				.setArgumentCountBetween( 1, 3 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_char" )
				.setArgumentCountBetween( 1, 3 )
				//always 2 args on HSQL and Postgres
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_date" )
				//always 2 args on HSQL and Postgres
				.setArgumentCountBetween( 1, 3 )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_timestamp" )
				//always 2 args on HSQL and Postgres
				.setArgumentCountBetween( 1, 3 )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.register();
	}

	public static void dateTimeTimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "date" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "time" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestamp" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.register();
	}

	public static void utcDateTimeTimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "utc_date" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "utc_time" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "utc_timestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.register();
	}

	public static void currentUtcdatetimetimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_utcdate" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_utctime" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_utctimestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.register();
	}

	public static void week_weekofyear(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "weekofyear" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "week", "weekofyear" );
	}

	/**
	 * Almost every database
	 */
	public static void concat_pipeOperator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().varArgsBuilder( "concat", "(", "||", ")" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setMinArgumentCount( 1 )
//				.setArgumentListSignature("(string0[, string1[, ...]])")
				.register();
	}

	/**
	 * Transact SQL-style
	 */
	public static void concat_plusOperator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().varArgsBuilder( "concat", "(", "+", ")" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setMinArgumentCount( 1 )
//				.setArgumentListSignature("(string0[, string1[, ...]])")
				.register();
	}

	/**
	 * Oracle-style
	 */
	public static void rownumRowid(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "rowid" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setUseParenthesesWhenNoArgs( false )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "rownum" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	/**
	 * H2/HSQL-style
	 */
	public static void rownum(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "rownum" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setUseParenthesesWhenNoArgs( true ) //H2 and HSQL require the parens
				.register();
	}

	/**
	 * CUBRID
	 */
	public static void rownumInstOrderbyGroupbyNum(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "rownum" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setUseParenthesesWhenNoArgs( false )
				.register();

		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "inst_num" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "orderby_num" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "groupby_num" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	/**
	 * MySQL/CUBRID
	 */
	public static void makedateMaketime(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "makedate" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(year, dayofyear)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "maketime" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 3 )
				.setArgumentListSignature("(hour, min, sec)")
				.register();
	}

	/**
	 * Postgres
	 */
	public static void makeDateTimeTimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_date" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_time" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_timestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 6 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "make_timestamptz" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 6, 7 )
				.register();
	}

	public static void sysdate(QueryEngine queryEngine) {
		// returns a local timestamp
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "sysdate" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	/**
	 * MySQL requires the parens in sysdate()
	 */
	public static void sysdateParens(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "sysdate" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	/**
	 * MySQL 5.7 precision defaults to seconds, but microseconds is better
	 */
	public static void sysdateExplicitMicros(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "sysdate", "sysdate(6)" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 0 )
				.register();
	}

	public static void systimestamp(QueryEngine queryEngine) {
		// returns a timestamp with timezone
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "systimestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	public static void localtimeLocaltimestamp(QueryEngine queryEngine) {
		//these functions return times without timezones
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "localtime" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setUseParenthesesWhenNoArgs( false )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "localtimestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setUseParenthesesWhenNoArgs( false )
				.register();

		queryEngine.getSqmFunctionRegistry().noArgsBuilder("current time", "localtime")
				.setInvariantType( StandardSpiBasicTypes.LOCAL_TIME )
				.setUseParenthesesWhenNoArgs( false )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder("current datetime", "localtimestamp")
				.setInvariantType( StandardSpiBasicTypes.LOCAL_DATE_TIME )
				.setUseParenthesesWhenNoArgs( false )
				.register();
	}

	public static void trigonometry(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("sin")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("cos")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("tan")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("asin")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("acos")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("atan")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("atan2")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount(2)
				.register();
	}

	/**
	 * Transact-SQL atan2 is misspelled
	 */
	public static void atan2_atn2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "atan2", "atn2")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void coalesce(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("coalesce")
				.setMinArgumentCount( 1 )
				.register();
	}

	/**
	 * SAP DB
	 */
	public static void coalesce_value(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("value")
				.setMinArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "coalesce", "value" );
	}

	public static void nullif(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("nullif")
				.setExactArgumentCount(2)
				.register();
	}

	/**
	 * ANSI SQL-style
	 */
	public static void length_characterLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("character_length")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "length", "character_length" );
	}

	/**
	 * Transact SQL-style
	 */
	public static void characterLength_len(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "len" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "character_length", "len" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "length", "len" );
	}

	/**
	 * Oracle-style
	 */
	public static void characterLength_length(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "length" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "character_length", "length" );
	}

	public static void octetLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("octet_length")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void bitLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("bit_length")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(1)
				.register();
	}

	/**
	 * ANSI-style
	 */
	public static void position(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("position", "position(?1 in ?2)")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(pattern in string)")
				.register();
	}

	public static void locate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("locate")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween(2, 3)
				.setArgumentListSignature("(pattern, string[, start])")
				.register();
	}

	/**
	 * Transact SQL-style
	 */
	public static void locate_charindex(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "charindex" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 3 )
				.setArgumentListSignature("(pattern, string[, start])")
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "locate", "charindex" );
	}

	/**
	 * Transact SQL-style (not the same as ANSI-style substring!)
	 */
	public static void substring(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("substring")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween(2, 3)
				.setArgumentListSignature("(string{ from|,} start[{ for|,} length])")
				.register();
	}

	/**
	 * Oracle, and many others
	 */
	public static void substring_substr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "substring", "substr" )
				.setArgumentListSignature("(string{ from|,} start[{ for|,} length])")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
	}

	public static void insert(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("insert")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(4)
				.setArgumentListSignature("(string, start, length, replacement)")
				.register();
	}

	/**
	 * Postgres
	 */
	public static void insert_overlay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("insert", "overlay(?1 placing ?4 from ?2 for ?3)")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(4)
				.setArgumentListSignature("(string, start, length, replacement)")
				.register();
	}

	/**
	 * ANSI SQL form, supported by Postgres, HSQL
	 */
	public static void overlay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerTernaryQuaternaryPattern("overlay", StandardSpiBasicTypes.STRING,
				"overlay(?1 placing ?2 from ?3)",
				"overlay(?1 placing ?2 from ?3 for ?4)")
				.setArgumentListSignature("(string placing replacement from start[ for length])");
	}

	/**
	 * For DB2 which has a broken implementation of overlay()
	 */
	public static void overlayCharacterLength_overlay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerTernaryQuaternaryPattern("overlay", StandardSpiBasicTypes.STRING,
				//use character_length() here instead of length()
				//because DB2 doesn't like "length(?)"
				"overlay(?1 placing ?2 from ?3 for character_length(?2))",
				"overlay(?1 placing ?2 from ?3 for ?4)")
				.setArgumentListSignature("(string placing replacement from start[ for length])");
	}

	public static void replace(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("replace")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(3)
				.setArgumentListSignature("(string, pattern, replacement)")
				.register();
	}

	/**
	 * Sybase
	 */
	public static void replace_strReplace(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("str_replace")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(3)
				.setArgumentListSignature("(string, pattern, replacement)")
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey("replace", "str_replace");
	}

	public static void concat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("concat")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setMinArgumentCount(1)
				.setArgumentListSignature("(string0[, string1[, ...]])")
				.register();
	}

	public static void lowerUpper(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("lower")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(1)
				.setArgumentListSignature("(string)")
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("upper")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(1)
				.setArgumentListSignature("(string)")
				.register();
	}

	public static void ascii(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ascii" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.INTEGER ) //should it be BYTE??
				.register();
	}

	public static void char_chr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "chr" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.CHARACTER )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "char", "chr" );
	}

	public static void chr_char(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "char" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.CHARACTER )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "chr", "char" );
	}

	/**
	 * Transact SQL-style
	 */
	public static void datepartDatename(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datepart" )
//				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(field, arg)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datename" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(field, arg)")
				.register();
	}

	public static void trim(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("trim", "trim(?1 ?2 from ?3)")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(3)
				.setArgumentListSignature("([[{leading|trailing|both} ][character ]from] string)")
				.register();
	}

	// No real consistency in the semantics of these functions:
	// H2, HSQL: now()/curtime()/curdate() mean localtimestamp/localtime/current_date
	// MySQL, Cache: now()/curtime()/curdate() mean current_timestamp/current_time/current_date
	// CUBRID: now()/curtime()/curdate() mean current_datetime/current_time/current_date
	// Postgres: now() means current_timestamp
	public static void nowCurdateCurtime(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "curtime" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "curdate" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "now" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setUseParenthesesWhenNoArgs( true )
				.register();
	}

	public static void leastGreatest(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "least" )
				.setMinArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "greatest" )
				.setMinArgumentCount( 1 )
				.register();
	}

	public static void leastGreatest_minMaxValue(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "least", "minValue" )
				.setMinArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "greatest", "mxValue" )
				.setMinArgumentCount( 1 )
				.register();
	}

	public static void aggregates(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("max")
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("min")
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("sum")
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("avg")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("count")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(1)
				.setArgumentListSignature("([distinct ]{arg|*})")
				.register();
	}

	public static void math(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "round" )
				.setExactArgumentCount(2)
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("floor")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("ceiling")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("mod")
				.setExactArgumentCount(2)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("abs")
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("sign")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("sqrt")
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("ln")
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("exp")
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("power")
				.setExactArgumentCount(2)
				.register();
	}

	public static void mod_operator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "mod", "(?1 % ?2)" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void power_expLn(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "power", "exp(ln(?1)*?2)")
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void square(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "square" )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void cbrt(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "cbrt" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void crc32(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "crc32" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void sha1(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sha1" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void sha2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sha2" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void sha(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sha" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * MySQL style, returns the number of days between two dates
	 */
	public static void datediff(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datediff" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(end, start)")
				.register();
	}

	/**
	 * MySQL style
	 */
	public static void adddateSubdateAddtimeSubtime(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "adddate" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, days)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "subdate" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, days)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "addtime" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, time)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "subtime" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, time)")
				.register();
	}

	public static void addMonths(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("add_months")
				.setReturnTypeResolver( useArgType(1) )
				.setArgumentListSignature("(datetime, months)")
				.setExactArgumentCount(2)
				.register();
	}

	public static void monthsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("months_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(end, start)")
				.register();
	}

	public static void daysBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("days_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(end, start)")
				.register();
	}

	public static void secondsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("seconds_between")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(end, start)")
				.register();
	}

	public static void yearsMonthsDaysHoursMinutesSecondsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("years_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(end, start)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("months_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(end, start)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("days_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(end, start)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("hours_between")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(end, start)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("minutes_between")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(end, start)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("seconds_between")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(end, start)")
				.register();
	}

	public static void addYearsMonthsDaysHoursMinutesSeconds(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_years" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, years)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_months" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, months)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_days" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, days)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_hours" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, hours)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_minutes" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, minutes)")
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_seconds" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime, seconds)")
				.register();
	}

	/**
	 * H2-style (uses Java's SimpleDateFormat directly so no need to translate format)
	 */
	public static void format_formatdatetime(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("format", "formatdatetime")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(datetime as pattern)")
				.register();
	}

	/**
	 * Usually Oracle-style (except for Informix which quite close to MySQL-style)
	 *
	 * @see org.hibernate.dialect.OracleDialect#datetimeFormat
	 * @see org.hibernate.dialect.InformixDialect#datetimeFormat
	 */
	public static void format_toChar(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("format", "to_char")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(datetime as pattern)")
				.register();
	}

	/**
	 * MySQL-style (also Ingres)
	 *
	 * @see org.hibernate.dialect.MySQLDialect#datetimeFormat
	 */
	public static void format_dateFormat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("format", "date_format")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(datetime as pattern)")
				.register();
	}

	/**
	 * SQL Server-style
	 *
	 * @see org.hibernate.dialect.SQLServerDialect#datetimeFormat
	 */
	public static void format_format(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("format", "format")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(datetime as pattern)")
				.register();
	}

	/**
	 * HANA's name for to_char() is still Oracle-style
	 *
	 *  @see org.hibernate.dialect.OracleDialect#datetimeFormat
	 */
	public static void format_toVarchar(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("format", "to_varchar")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(datetime as pattern)")
				.register();
	}

	public static void dateTrunc(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("date_trunc", "date_trunc('?1',?2)")
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount(2)
				.setArgumentListSignature("(field, datetime)")
				.register();
	}

}
