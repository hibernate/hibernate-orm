/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.type.StandardBasicTypes;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useFirstNonNull;

/**
 * A group common function template definitions.  Centralized for easier use from
 * Dialects
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@SuppressWarnings("unused")
public class CommonFunctionFactory {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// trigonometric/geometric functions

	public static void cosh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "cosh" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void cot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "cot" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}

	public static void degrees(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "degrees" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}

	public static void log(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "log" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}

	public static void log10(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "log10" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}

	public static void log2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "log2" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void radians(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "radians" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}

	public static void sinh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sinh" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}

	public static void tanh(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "tanh" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basic math functions

	public static void trunc(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "trunc" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}

	public static void truncate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "truncate" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}

	public static void rand(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "rand" )
				.setArgumentCountBetween( 0, 1 )
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
	}

	public static void rand_random(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "random" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setUseParenthesesWhenNoArgs( true )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "rand", "random" );
	}

	public static void stddev(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "stddev" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void variance(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "variance" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void pi(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "pi", StandardBasicTypes.DOUBLE );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// character functions

	public static void soundex(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "soundex" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.STRING )
				.register();
	}

	public static void trim2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ltrim" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "rtrim" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
	}

	public static void trim1(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ltrim" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "rtrim" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void pad(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "lpad" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "rpad" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
	}

	public static void pad_fill(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"lpad",
				StandardBasicTypes.STRING,
				"lfill(?1,' ',?2)",
				"lfill(?1,?3,?2)"
		);
		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"rpad",
				StandardBasicTypes.STRING,
				"rfill(?1,' ',?2)",
				"rfill(?1,?3,?2)"
		);
	}

	public static void reverse(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "reverse" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void space(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "space" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void repeat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "repeat" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void leftRight(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "left" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "right" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void repeat_replicate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "replicate" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "repeat", "replicate" );
	}

	public static void md5(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "md5" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void initcap(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "initcap" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void instr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "instr" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 4 )
				.register();
	}

	public static void substring_substr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "substr" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "substring", "substr" );
	}

	public static void locate_charindex(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "charindex" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "locate", "charindex" );
	}


	public static void translate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "translate" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 3 )
				.register();
	}

	public static void bitand(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bitand" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitor(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bitand" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitxor(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bitand" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitnot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bitnot" )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void yearMonthDay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "day" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "month" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "year" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void hourMinuteSecond(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "hour" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "minute" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "second" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "microsecond" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void dayofweekmonthyear(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "dayofweek" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "dayofmonth" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "day", "dayofmonth" );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "dayofyear" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void dayOfWeekMonthYear(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "day_of_week" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "day_of_month" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "day", "day_of_month" );
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "day_of_year" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void daynameMonthname(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "monthname" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "dayname" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void weekQuarter(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "week" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "quarter" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.register();
	}

	public static void lastDay(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "last_day" )
				.setInvariantType( StandardBasicTypes.DATE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void lastDay_eomonth(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "eomonth" )
				.setInvariantType( StandardBasicTypes.DATE )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "last_date", "eomonth" );
	}

	public static void ceiling_ceil(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ceil" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "ceiling", "ceil" );
	}

	public static void toCharNumberDateTimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "to_char" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardBasicTypes.STRING )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "to_number" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "to_date" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "to_timestamp" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.register();
	}

	public static void dateTimeTimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "date" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "time" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.TIME )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "timestamp" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.register();
	}

	public static void utcDateTimeTimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "utc_date" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "utc_time" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardBasicTypes.TIME )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "utc_timestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.register();
	}

	public static void currentUtcdatetimetimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_utcdate" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_utctime" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardBasicTypes.TIME )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_utctimestamp" )
				.setUseParenthesesWhenNoArgs( false )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.register();
	}

	public static void week_weekofyear(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "weekofyear" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "week", "weekofyear" );
	}

	public static void concat_operator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerVarArgs( "concat", StandardBasicTypes.STRING, "(", "||", ")" );
	}

	public static void rownumRowid(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "rowid", StandardBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "rownum", StandardBasicTypes.LONG );
	}

	public static void sysdateSystimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "sysdate", StandardBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "systimestamp", StandardBasicTypes.TIMESTAMP );
	}

	public static void localtimeLocaltimestamp(QueryEngine queryEngine) {
		//these functions return times without timezones
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "localtime", StandardBasicTypes.TIME );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "localtimestamp", StandardBasicTypes.TIMESTAMP );
	}

	public static void trigonometry(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sin" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "cos" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "tan" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "asin" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "acos" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "atan" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "atan2" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void coalesce(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "coalesce" )
				.setArgumentsValidator( StandardArgumentsValidators.min( 1 ) )
				.register();
	}

	public static void coalesce_value(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "value" )
				.setArgumentsValidator( StandardArgumentsValidators.min( 1 ) )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "coalesce", "value" );
	}

	public static void nullif(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "nullif" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void characterLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "character_length" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void octetLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "octet_length" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void characterLength_len(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "len" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "character_length", "len" );
	}

	public static void characterLength_length(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "length" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "character_length", "length" );
	}

	public static void bitLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "bit_length" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void locate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "locate" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 3 )
				.register();
	}

	public static void substring(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "substring" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
	}

	public static void replace(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "replace" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 3 )
				.register();
	}

	public static void replace_strReplace(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "str_replace" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "replace", "str_replace" );
	}

	public static void concat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "concat" )
				.setInvariantType( StandardBasicTypes.STRING )
				.register();
	}

	public static void lowerUpper(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "lower" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "upper" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void ascii(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ascii" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.INTEGER ) //should it be BYTE??
				.register();
	}

	public static void char_chr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "chr" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.CHARACTER )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "char", "chr" );
	}

	public static void chr_char(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "char" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.CHARACTER )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "chr", "char" );
	}

	public static void extract(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "extract", "extract(?1 from ?2)" )
//				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.setReturnTypeResolver( useArgType( 1 ) )
				.register();
	}

	public static void extract_datepart(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datepart" )
//				.setInvariantType( StandardBasicTypes.INTEGER )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datename" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "extract", "datepart " );
	}

	public static void cast(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "cast", "cast(?1 as ?2)" )
				.setExactArgumentCount( 2 )
				.setReturnTypeResolver( useArgType( 2 ) )
				.register();
	}

	public static void trim(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "trim", "trim(?1 ?2 from ?3)" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 3 )
				.register();
	}

	public static void currentDateTimeTimestamp(QueryEngine queryEngine) {
		// Legacy JDK `Date`-based functions
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_time" )
				.setInvariantType( StandardBasicTypes.TIME )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_date" )
				.setInvariantType( StandardBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_timestamp" )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.register();

		// "JDK 8" temporal-type functions.
		// 		- These are essentially aliases for the `current_XYZ` forms
		//		but defining JDK 8 temporal type return values
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_time" )
				.setInvariantType( StandardBasicTypes.LOCAL_TIME )
				.register( "local_time" );
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_date" )
				.setInvariantType( StandardBasicTypes.LOCAL_DATE )
				.register( "local_date" );
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_timestamp" )
				.setInvariantType( StandardBasicTypes.LOCAL_DATE_TIME )
				.register( "local_datetime");
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "current_timestamp" )
				.setInvariantType( StandardBasicTypes.INSTANT )
				.register( "current_instant" );

		//these are synonyms on many databases, so for convenience register them here
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "now", "current_timestamp" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "curdate", "current_date" );
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "curtime", "current_time" );
	}

	/**
	 * For databases like MySQL which default to truncating the the fractional seconds.
	 */
	public static void currentTimestampExplicitMicros(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "current_timestamp", "current_timestamp(6)" )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 0 )
				.register();
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "current datetime", "current_timestamp(6)" )
				.setInvariantType( StandardBasicTypes.LOCAL_DATE_TIME )
				.setExactArgumentCount( 0 )
				.register();
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "current instant", "current_timestamp(6)" )
				.setInvariantType( StandardBasicTypes.INSTANT )
				.setExactArgumentCount( 0 )
				.register();
	}

	public static void leastGreatest(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "least" )
				.setReturnTypeResolver( useFirstNonNull() )
				.setArgumentsValidator( StandardArgumentsValidators.min( 1 ) )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "greatest" )
				.setReturnTypeResolver( useFirstNonNull() )
				.setArgumentsValidator( StandardArgumentsValidators.min( 1 ) )
				.register();
	}

	public static void aggregates(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "max" )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useFirstNonNull() )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "min" )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useFirstNonNull() )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sum" )
				.setReturnTypeResolver( StandardFunctionReturnTypeResolvers.useFirstNonNull() )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "avg" )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "count" )
				.setInvariantType( StandardBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void math(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "round" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardBasicTypes.DOUBLE )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "floor" )
				.setInvariantType( StandardBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "ceiling" )
				.setInvariantType( StandardBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "mod" )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "abs" )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sign" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sqrt" )
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
				.register();
	}

	public static void crc32(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "crc32" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void sha1sha2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sha2" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sha1" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void sha(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "sha" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	/**
	 * MySQL style, returns the number of days between two dates
	 */
	public static void datediff(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "datediff" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * MySQL style
	 */
	public static void adddateSubdateAddtimeSubtime(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "adddate" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "subdate" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "addtime" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "subtime" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void addMonths(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_months" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void monthsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "months_between" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void daysBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "days_between" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void secondsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "seconds_between" )
				.setInvariantType( StandardBasicTypes.LONG )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void yearsMonthsDaysHoursMinutesSecondsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "years_between" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "months_between" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "days_between" )
				.setInvariantType( StandardBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "hours_between" )
				.setInvariantType( StandardBasicTypes.LONG )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "minutes_between" )
				.setInvariantType( StandardBasicTypes.LONG )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "seconds_between" )
				.setInvariantType( StandardBasicTypes.LONG )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void addYearsMonthsDaysHoursMinutesSeconds(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_years" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_months" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_days" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_hours" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_minutes" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "add_seconds" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * H2-style (uses Java's SimpleDateFormat directly so no need to translate format)
	 */
	public static void formatdatetime(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "formatdatetime" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * Usually Oracle-style (except for Informix which quite close to MySQL-style)
	 */
	public static void formatdatetime_toChar(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "formatdatetime", "to_char" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * MySQL-style (also Ingres)
	 */
	public static void formatdatetime_dateFormat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "formatdatetime", "date_format" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * SQL Server-style
	 */
	public static void formatdatetime_format(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "formatdatetime", "format" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * HANA's name for to_char() is still Oracle-style
	 */
	public static void formatdatetime_toVarchar(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "formatdatetime", "to_varchar" )
				.setInvariantType( StandardBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void dateTrunc(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternDescriptorBuilder( "date_trunc", "date_trunc('?1',?2)" )
				.setInvariantType( StandardBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 2 )
				.register();
	}

}
