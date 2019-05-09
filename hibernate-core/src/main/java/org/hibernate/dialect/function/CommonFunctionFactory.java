/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.spi.PairedFunctionTemplate;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import static org.hibernate.query.sqm.produce.function.StandardArgumentsValidators.min;
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basic math functions

	public static void trunc(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "trunc" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void truncate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "truncate" )
				.setArgumentCountBetween( 1, 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void rand(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "rand" )
				.setArgumentCountBetween( 0, 1 )
				.setUseParenthesesWhenNoArgs( true )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
	}

	public static void rand_random(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder( "random")
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setUseParenthesesWhenNoArgs(true)
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "rand", "random" );
	}

	public static void stddev(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "stddev" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void variance(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "variance" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void pi(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "pi", StandardSpiBasicTypes.DOUBLE );
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
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "rtrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
	}

	public static void trim1(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ltrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "rtrim" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void pad(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "lpad" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "rpad" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
	}

	public static void pad_fill(QueryEngine queryEngine) {
		PairedFunctionTemplate.register(queryEngine, "lpad", StandardSpiBasicTypes.STRING, "lfill(?1,' ',?2)", "lfill(?1,?3,?2)");
		PairedFunctionTemplate.register(queryEngine, "rpad", StandardSpiBasicTypes.STRING, "rfill(?1,' ',?2)", "rfill(?1,?3,?2)");
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
				.register();
	}

	public static void leftRight(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "left" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "right" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void repeat_replicate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "replicate" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
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
				.register();
	}

	public static void substring_substr(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "substr" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "substring", "substr" );
	}

	public static void locate_charindex(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "charindex" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "locate", "charindex" );
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
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bitand" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitxor(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bitand" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void bitnot(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "bitnot" )
				.setExactArgumentCount( 1 )
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
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_char" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_number" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_date" )
				.setExactArgumentCount( 2 )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_timestamp" )
				.setExactArgumentCount( 2 )
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

	public static void week_weekofyear(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "weekofyear" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "week", "weekofyear" );
	}

	public static void concat_operator(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerVarArgs( "concat", StandardSpiBasicTypes.STRING, "(", "||", ")" );
	}

	public static void rownumRowid(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "rowid", StandardSpiBasicTypes.LONG );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "rownum", StandardSpiBasicTypes.LONG );
	}

	public static void sysdateSystimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "sysdate", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "systimestamp", StandardSpiBasicTypes.TIMESTAMP );
	}

	public static void localtimeLocaltimestamp(QueryEngine queryEngine) {
		//these functions return times without timezones
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "localtime", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "localtimestamp", StandardSpiBasicTypes.TIMESTAMP );
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

	public static void coalesce(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("coalesce")
				.setArgumentsValidator( min(1) )
				.register();
	}

	public static void coalesce_value(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("value")
				.setArgumentsValidator( min(1) )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "coalesce", "value" );
	}

	public static void nullif(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("nullif")
				.setExactArgumentCount(2)
				.register();
	}

	public static void characterLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("character_length")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void octetLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("octet_length")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void characterLength_len(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "len" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "character_length", "len" );
	}

	public static void characterLength_length(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "length" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "character_length", "length" );
	}

	public static void bitLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("bit_length")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(1)
				.register();
	}

	public static void locate(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("locate")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setArgumentCountBetween(2, 3)
				.register();
	}

	public static void substring(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("substring")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween(2, 3)
				.register();
	}

	public static void replace(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("replace")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(3)
				.register();
	}

	public static void replace_strReplace(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("str_replace")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(3)
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey("replace", "str_replace");
	}

	public static void concat(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("concat")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();
	}

	public static void lowerUpper(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("lower")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(1)
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("upper")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(1)
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

	public static void extract(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("extract", "extract(?1 from ?2)")
				.setExactArgumentCount(2)
				.setReturnTypeResolver( useArgType(1) )
				.register();
	}

	public static void extract_datepart(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datepart" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datename" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "extract", "datepart ");
	}

	public static void cast(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("cast", "cast(?1 as ?2)")
				.setExactArgumentCount(2)
				.setReturnTypeResolver( useArgType(2) )
				.register();
	}

	public static void trim(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("trim", "trim(?1 ?2 from ?3)")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount(3)
				.register();
	}

	public static void currentDateTimeTimestamp(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().noArgsBuilder("current_time")
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder("current_date")
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.register();
		queryEngine.getSqmFunctionRegistry().noArgsBuilder("current_timestamp") //current_instant uses this
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.register();

		//these are synonyms on many databases, so for convenience register them here
		queryEngine.getSqmFunctionRegistry().registerAlternateKey("now", "current_timestamp");
		queryEngine.getSqmFunctionRegistry().registerAlternateKey("curdate", "current_date");
		queryEngine.getSqmFunctionRegistry().registerAlternateKey("curtime", "current_time");
	}

	public static void leastGreatest(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "least" )
				.setArgumentsValidator( min(1) )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "greatest" )
				.setArgumentsValidator( min(1) )
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

	public static void crc32(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "crc32" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void sha1sha2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sha2" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sha1" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void sha(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sha" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void timestampadd(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("timestampadd")
				.setReturnTypeResolver(useArgType(3))
				.setExactArgumentCount( 3 )
				.register();
	}

	public static void timestampdiff(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestampdiff" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 3 )
				.register();
	}

	/**
	 * Transact SQL style, accepts (datepart, int, datetime)
	 * and returns a datetime type
	 */
	public static void timestampadd_dateadd(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "dateadd" )
				.setReturnTypeResolver( useArgType(3) )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "timestampadd", "dateadd" );
	}

	/**
	 * Transact SQL style, accepts (datepart, startdatetime, enddatetime)
	 * and returns an int
	 */
	public static void timestampdiff_datediff(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datediff" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "timestampdiff", "datediff" );
	}

	/**
	 * MySQL style, returns the number of days between two dates
	 */
	public static void datediff2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datediff" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 2 )
				.register();
	}

	/**
	 * MySQL style
	 */
	public static void adddateSubdateAddtimeSubtime(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "adddate" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "subdate" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "addtime" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "subtime" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void addMonths(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("add_months")
				.setReturnTypeResolver(useArgType(1))
				.setExactArgumentCount(2)
				.register();
	}

	public static void monthsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("months_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.register();
	}

	public static void daysBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("days_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.register();
	}

	public static void secondsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("seconds_between")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(2)
				.register();
	}

	public static void yearsMonthsDaysHoursMinutesSecondsBetween(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("years_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("months_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("days_between")
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount(2)
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("hours_between")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(2)
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("minutes_between")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(2)
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("seconds_between")
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount(2)
				.register();
	}

	public static void addYearsMonthsDaysHoursMinutesSeconds(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_years" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_months" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_days" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_hours" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_minutes" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "add_seconds" )
				.setReturnTypeResolver( useArgType(1) )
				.setExactArgumentCount( 2 )
				.register();
	}

}
