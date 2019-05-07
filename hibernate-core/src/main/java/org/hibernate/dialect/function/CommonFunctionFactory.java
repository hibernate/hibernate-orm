/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import static org.hibernate.query.sqm.produce.function.StandardArgumentsValidators.min;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;

/**
 * A group common function template definitions.  Centralized for easier use from
 * Dialects
 *
 * @author Steve Ebersole
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

	public static void octetLength(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "octet_length" )
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
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

	public static void nvl(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "nvl" )
				.setExactArgumentCount( 2 )
				.register();
	}

	public static void nvl2(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "nvl2" )
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

	public static void ifnull(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ifnull" )
				.setExactArgumentCount( 2 )
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
				.setInvariantType( StandardSpiBasicTypes.INTEGER )
				.setExactArgumentCount( 1 )
				.register();
	}

	public static void chr_char(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "char" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.CHARACTER )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "chr", "char" );
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

	public static void locate(QueryEngine queryEngine, String pattern2, String pattern3) {
		queryEngine.getSqmFunctionRegistry().register(
				"locate",
				new LocateEmulation(
						queryEngine.getSqmFunctionRegistry()
								.patternTemplateBuilder( "locate/2", pattern2 )
								.setExactArgumentCount( 2 )
								.setInvariantType( StandardSpiBasicTypes.INTEGER )
								.register(),
						queryEngine.getSqmFunctionRegistry()
								.patternTemplateBuilder( "locate/3", pattern3 )
								.setExactArgumentCount( 3 )
								.setInvariantType( StandardSpiBasicTypes.INTEGER )
								.register()
				)
		);
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
                .register();
    }

    public static void nullif(QueryEngine queryEngine) {
        queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("nullif")
                .setExactArgumentCount(2)
                .register();
    }

    public static void characterLength(QueryEngine queryEngine) {
        queryEngine.getSqmFunctionRegistry().namedTemplateBuilder("character_length") //length() is a synonym
                .setInvariantType( StandardSpiBasicTypes.INTEGER )
                .setExactArgumentCount(1)
                .register();
        //this is a synonym on many databases
        queryEngine.getSqmFunctionRegistry().registerAlternateKey("char_length", "character_length");

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

    public static void chr(QueryEngine queryEngine) {
        queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "chr" )
                .setExactArgumentCount( 1 )
                .setInvariantType( StandardSpiBasicTypes.CHARACTER )
                .register();
    }

    public static void extract(QueryEngine queryEngine) {
        queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("extract", "extract(?1 from ?2)")
                .setExactArgumentCount(2)
                .setReturnTypeResolver( useArgType(1) )
                .register();
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

    public static void datetime_extract(QueryEngine queryEngine) {
        //TODO: re-express these in terms of registered extract() function so
        //      they don't need to be redefined by dialect subclasses
        queryEngine.getSqmFunctionRegistry().registerPattern( "second", "extract(second from ?1)" );
        queryEngine.getSqmFunctionRegistry().registerPattern( "minute", "extract(minute from ?1)" );
        queryEngine.getSqmFunctionRegistry().registerPattern( "hour", "extract(hour from ?1)" );
        queryEngine.getSqmFunctionRegistry().registerPattern( "day", "extract(day from ?1)" );
        queryEngine.getSqmFunctionRegistry().registerPattern( "month", "extract(month from ?1)" );
        queryEngine.getSqmFunctionRegistry().registerPattern( "year", "extract(year from ?1)" );
    }

    public static void str_cast(QueryEngine queryEngine) {
        //TODO: re-express this in terms of registered cast() function so
        //      it doesn't need to be redefined by dialect subclasses
        queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "str", "cast(?1 as char)" )
                .setInvariantType( StandardSpiBasicTypes.STRING )
                .setExactArgumentCount(1)
                .register();
    }
}
