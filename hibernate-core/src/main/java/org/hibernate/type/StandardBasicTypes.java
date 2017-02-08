/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.boot.model.type.spi.BasicTypeProducerRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Centralizes access to the standard set of basic {@link Type types}.
 * <p/>
 * Type mappings can be adjusted per {@link org.hibernate.SessionFactory} (technically per
 * {@link TypeConfiguration}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
public final class StandardBasicTypes {
	private StandardBasicTypes() {
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// boolean data

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#BIT BIT}.
	 */
	public static final Type<Boolean> BOOLEAN = org.hibernate.type.spi.StandardBasicTypes.BOOLEAN;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 */
	public static final Type<Boolean> NUMERIC_BOOLEAN = org.hibernate.type.spi.StandardBasicTypes.NUMERIC_BOOLEAN;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'T'/'F').
	 */
	public static final Type<Boolean> TRUE_FALSE = org.hibernate.type.spi.StandardBasicTypes.TRUE_FALSE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'Y'/'N').
	 */
	public static final Type<Boolean> YES_NO = org.hibernate.type.spi.StandardBasicTypes.YES_NO;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// byte/binary data

	/**
	 * The standard Hibernate type for mapping {@link Byte} to JDBC {@link java.sql.Types#TINYINT TINYINT}.
	 */
	public static final Type<Byte> BYTE = org.hibernate.type.spi.StandardBasicTypes.BYTE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 */
	public static final Type<byte[]> BINARY = org.hibernate.type.spi.StandardBasicTypes.BINARY;

	/**
	 * The standard Hibernate type for mapping {@link Byte Byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 */
	public static final Type<Byte[]> WRAPPER_BINARY = org.hibernate.type.spi.StandardBasicTypes.WRAPPER_BINARY;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY}.
	 *
	 * @see #MATERIALIZED_BLOB
	 */
	public static final Type<byte[]> IMAGE = org.hibernate.type.spi.StandardBasicTypes.IMAGE;
	/**
	 * The standard Hibernate type for mapping {@link java.sql.Blob} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 */
	public static final Type<Blob> BLOB = org.hibernate.type.spi.StandardBasicTypes.BLOB;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final Type<byte[]> MATERIALIZED_BLOB = org.hibernate.type.spi.StandardBasicTypes.MATERIALIZED_BLOB;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// numeric data

	/**
	 * The standard Hibernate type for mapping {@link Short} to JDBC {@link java.sql.Types#SMALLINT SMALLINT}.
	 */
	public static final Type<Short> SHORT = org.hibernate.type.spi.StandardBasicTypes.SHORT;

	/**
	 * The standard Hibernate type for mapping {@link Integer} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 */
	public static final Type<Integer> INTEGER = org.hibernate.type.spi.StandardBasicTypes.INTEGER;
	/**
	 * The standard Hibernate type for mapping {@link Long} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 */
	public static final Type<Long> LONG = org.hibernate.type.spi.StandardBasicTypes.LONG;

	/**
	 * The standard Hibernate type for mapping {@link Float} to JDBC {@link java.sql.Types#FLOAT FLOAT}.
	 */
	public static final Type<Float> FLOAT = org.hibernate.type.spi.StandardBasicTypes.FLOAT;

	/**
	 * The standard Hibernate type for mapping {@link Double} to JDBC {@link java.sql.Types#DOUBLE DOUBLE}.
	 */
	public static final Type<Double> DOUBLE = org.hibernate.type.spi.StandardBasicTypes.DOUBLE;

	/**
	 * The standard Hibernate type for mapping {@link java.math.BigInteger} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 */
	public static final Type<BigInteger> BIG_INTEGER = org.hibernate.type.spi.StandardBasicTypes.BIG_INTEGER;

	/**
	 * The standard Hibernate type for mapping {@link java.math.BigDecimal} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 */
	public static final Type<BigDecimal> BIG_DECIMAL = org.hibernate.type.spi.StandardBasicTypes.BIG_DECIMAL;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// String / character data

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#CHAR CHAR(1)}.
	 */
	public static final Type<Character> CHARACTER = org.hibernate.type.spi.StandardBasicTypes.CHARACTER;

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#NCHAR NCHAR(1)}.
	 */
	public static final Type<Character> CHARACTER_NCHAR = org.hibernate.type.spi.StandardBasicTypes.CHARACTER_NCHAR;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final Type<String> STRING = org.hibernate.type.spi.StandardBasicTypes.STRING;

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final Type<char[]> CHAR_ARRAY = org.hibernate.type.spi.StandardBasicTypes.CHAR_ARRAY;

	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC
	 * {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final Type<Character[]> CHARACTER_ARRAY = org.hibernate.type.spi.StandardBasicTypes.CHARACTER_ARRAY;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_CLOB}
	 */
	public static final Type<String> TEXT = org.hibernate.type.spi.StandardBasicTypes.TEXT;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGNVARCHAR LONGNVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_NCLOB}
	 */
	public static final Type<String> NTEXT = org.hibernate.type.spi.StandardBasicTypes.NTEXT;

	/**
	 * The standard Hibernate type for mapping {@link java.sql.Clob} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 */
	public static final Type<Clob> CLOB = org.hibernate.type.spi.StandardBasicTypes.CLOB;

	/**
	 * The standard Hibernate type for mapping {@link java.sql.NClob} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_NCLOB
	 */
	public static final Type<NClob> NCLOB = org.hibernate.type.spi.StandardBasicTypes.NCLOB;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #TEXT
	 */
	public static final Type<String> MATERIALIZED_CLOB = org.hibernate.type.spi.StandardBasicTypes.MATERIALIZED_CLOB;

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final Type<String> MATERIALIZED_CLOB_CHAR_ARRAY = org.hibernate.type.spi.StandardBasicTypes.MATERIALIZED_CLOB_CHAR_ARRAY;

	/**
	 * The standard Hibernate type for mapping {@code Character[]} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final Type<String> MATERIALIZED_CLOB_CHARACTER_ARRAY = org.hibernate.type.spi.StandardBasicTypes.MATERIALIZED_CLOB_CHARACTER_ARRAY;
	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #NTEXT
	 */
	public static final Type<String> MATERIALIZED_NCLOB = org.hibernate.type.spi.StandardBasicTypes.MATERIALIZED_NCLOB;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Date / time data

	/**
	 * The standard Hibernate type for mapping {@link Duration} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 */
	public static final Type<Duration> DURATION = org.hibernate.type.spi.StandardBasicTypes.DURATION;

	/**
	 * The standard Hibernate type for mapping {@link LocalDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final Type<LocalDateTime> LOCAL_DATE_TIME = org.hibernate.type.spi.StandardBasicTypes.LOCAL_DATE_TIME;

	/**
	 * The standard Hibernate type for mapping {@link LocalDate} to JDBC {@link java.sql.Types#DATE DATE}.
	 */
	public static final Type<LocalDate> LOCAL_DATE = org.hibernate.type.spi.StandardBasicTypes.LOCAL_DATE;

	/**
	 * The standard Hibernate type for mapping {@link LocalTime} to JDBC {@link java.sql.Types#TIME TIME}.
	 */
	public static final Type<LocalTime> LOCAL_TIME = org.hibernate.type.spi.StandardBasicTypes.LOCAL_TIME;

	/**
	 * The standard Hibernate type for mapping {@link OffsetDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final Type<OffsetDateTime> OFFSET_DATE_TIME = org.hibernate.type.spi.StandardBasicTypes.OFFSET_DATE_TIME;

	/**
	 * The standard Hibernate type for mapping {@link OffsetTime} to JDBC {@link java.sql.Types#TIME TIME}.
	 */
	public static final Type<OffsetTime> OFFSET_TIME = org.hibernate.type.spi.StandardBasicTypes.OFFSET_TIME;
	/**
	 * The standard Hibernate type for mapping {@link ZonedDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final Type<ZonedDateTime> ZONED_DATE_TIME = org.hibernate.type.spi.StandardBasicTypes.ZONED_DATE_TIME;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Time}) to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 */
	public static final Type<Date> TIME = org.hibernate.type.spi.StandardBasicTypes.TIME;
	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Date}) to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 */
	public static final Type<Date> DATE = org.hibernate.type.spi.StandardBasicTypes.DATE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Timestamp}) to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final Type<Date> TIMESTAMP = org.hibernate.type.spi.StandardBasicTypes.TIMESTAMP;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Calendar} to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final Type<Calendar> CALENDAR = org.hibernate.type.spi.StandardBasicTypes.CALENDAR;
	/**
	 * The standard Hibernate type for mapping {@link java.util.Calendar} to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 */
	public static final Type<Calendar> CALENDAR_DATE = org.hibernate.type.spi.StandardBasicTypes.CALENDAR_DATE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Calendar} to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 */
	public static final Type<Calendar> CALENDAR_TIME = org.hibernate.type.spi.StandardBasicTypes.CALENDAR_TIME;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UUID data

	/**
	 * The standard Hibernate type for mapping {@link java.util.UUID} to JDBC {@link java.sql.Types#BINARY BINARY}.
	 */
	public static final Type<UUID> UUID_BINARY = org.hibernate.type.spi.StandardBasicTypes.UUID_BINARY;

	/**
	 * The standard Hibernate type for mapping {@link java.util.UUID} to JDBC {@link java.sql.Types#CHAR CHAR}.
	 */
	public static final Type<UUID> UUID_CHAR = org.hibernate.type.spi.StandardBasicTypes.UUID_CHAR;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc data

	/**
	 * The standard Hibernate type for mapping {@link Class} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final Type<Class> CLASS = org.hibernate.type.spi.StandardBasicTypes.CLASS;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Currency} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final Type<Currency> CURRENCY = org.hibernate.type.spi.StandardBasicTypes.CURRENCY;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Locale} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final Type<Locale> LOCALE = org.hibernate.type.spi.StandardBasicTypes.LOCALE;

	/**
	 * The standard Hibernate type for mapping {@link java.io.Serializable} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 */
	public static final Type<Serializable> SERIALIZABLE = org.hibernate.type.spi.StandardBasicTypes.SERIALIZABLE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.TimeZone} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final Type<TimeZone> TIMEZONE = org.hibernate.type.spi.StandardBasicTypes.TIMEZONE;
	/**
	 * The standard Hibernate type for mapping {@link java.net.URL} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final Type<java.net.URL> URL = org.hibernate.type.spi.StandardBasicTypes.URL;

	public static void prime(TypeConfiguration typeConfiguration, BasicTypeProducerRegistry basicTypeProducerRegistry) {
		org.hibernate.type.spi.StandardBasicTypes.prime( typeConfiguration, basicTypeProducerRegistry );
	}

}
