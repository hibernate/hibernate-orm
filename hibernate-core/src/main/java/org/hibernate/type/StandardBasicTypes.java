/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.type.spi.TypeConfiguration;

/**
 * References to common instances of {@link BasicTypeReference}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class StandardBasicTypes {

	private StandardBasicTypes() {
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Boolean mappings

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link org.hibernate.type.SqlTypes#BOOLEAN BOOLEAN}.
	 */
	public static final BasicTypeReference<Boolean> BOOLEAN = new BasicTypeReference<>(
			"boolean",
			Boolean.class,
			SqlTypes.BOOLEAN
	);

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link org.hibernate.type.SqlTypes#TINYINT TINYINT}.
	 */
	public static final BasicTypeReference<Boolean> NUMERIC_BOOLEAN = new BasicTypeReference<>(
			"numeric_boolean",
			Boolean.class,
			SqlTypes.TINYINT,
			NumericBooleanConverter.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link org.hibernate.type.SqlTypes#CHAR CHAR(1)} (using 'T'/'F').
	 */
	public static final BasicTypeReference<Boolean> TRUE_FALSE = new BasicTypeReference<>(
			"true_false",
			Boolean.class,
			SqlTypes.CHAR,
			TrueFalseConverter.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link org.hibernate.type.SqlTypes#CHAR CHAR(1)} (using 'Y'/'N').
	 */
	public static final BasicTypeReference<Boolean> YES_NO = new BasicTypeReference<>(
			"yes_no",
			Boolean.class,
			SqlTypes.CHAR,
			YesNoConverter.INSTANCE
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Numeric mappings

	/**
	 * The standard Hibernate type for mapping {@link Byte} to JDBC {@link org.hibernate.type.SqlTypes#TINYINT TINYINT}.
	 */
	public static final BasicTypeReference<Byte> BYTE = new BasicTypeReference<>(
			"byte",
			Byte.class,
			SqlTypes.TINYINT
	);

	/**
	 * The standard Hibernate type for mapping {@link Short} to JDBC {@link org.hibernate.type.SqlTypes#SMALLINT SMALLINT}.
	 */
	public static final BasicTypeReference<Short> SHORT = new BasicTypeReference<>(
			"short",
			Short.class,
			SqlTypes.SMALLINT
	);

	/**
	 * The standard Hibernate type for mapping {@link Integer} to JDBC {@link org.hibernate.type.SqlTypes#INTEGER INTEGER}.
	 */
	public static final BasicTypeReference<Integer> INTEGER = new BasicTypeReference<>(
			"integer",
			Integer.class,
			SqlTypes.INTEGER
	);

	/**
	 * The standard Hibernate type for mapping {@link Long} to JDBC {@link org.hibernate.type.SqlTypes#BIGINT BIGINT}.
	 */
	public static final BasicTypeReference<Long> LONG = new BasicTypeReference<>(
			"long",
			Long.class,
			SqlTypes.BIGINT
	);

	/**
	 * The standard Hibernate type for mapping {@link Float} to JDBC {@link org.hibernate.type.SqlTypes#FLOAT FLOAT}.
	 */
	public static final BasicTypeReference<Float> FLOAT = new BasicTypeReference<>(
			"float",
			Float.class,
			SqlTypes.FLOAT
	);

	/**
	 * The standard Hibernate type for mapping {@link Double} to JDBC {@link org.hibernate.type.SqlTypes#DOUBLE DOUBLE}.
	 */
	public static final BasicTypeReference<Double> DOUBLE = new BasicTypeReference<>(
			"double",
			Double.class,
			SqlTypes.DOUBLE
	);

	/**
	 * The standard Hibernate type for mapping {@link BigInteger} to JDBC {@link org.hibernate.type.SqlTypes#NUMERIC NUMERIC}.
	 */
	public static final BasicTypeReference<BigInteger> BIG_INTEGER = new BasicTypeReference<>(
			"big_integer",
			BigInteger.class,
			SqlTypes.NUMERIC
	);

	/**
	 * The standard Hibernate type for mapping {@link BigDecimal} to JDBC {@link org.hibernate.type.SqlTypes#NUMERIC NUMERIC}.
	 */
	public static final BasicTypeReference<BigDecimal> BIG_DECIMAL = new BasicTypeReference<>(
			"big_decimal",
			BigDecimal.class,
			SqlTypes.NUMERIC
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Character mappings

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link org.hibernate.type.SqlTypes#CHAR CHAR(1)}.
	 */
	public static final BasicTypeReference<Character> CHARACTER = new BasicTypeReference<>(
			"character",
			Character.class,
			SqlTypes.CHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link org.hibernate.type.SqlTypes#NCHAR NCHAR(1)}.
	 */
	public static final BasicTypeReference<Character> CHARACTER_NCHAR = new BasicTypeReference<>(
			"ncharacter",
			Character.class,
			SqlTypes.NCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link org.hibernate.type.SqlTypes#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<String> STRING = new BasicTypeReference<>(
			"string",
			String.class,
			SqlTypes.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link org.hibernate.type.SqlTypes#NVARCHAR NVARCHAR}
	 */
	public static final BasicTypeReference<String> NSTRING = new BasicTypeReference<>(
			"nstring",
			String.class,
			SqlTypes.NVARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link org.hibernate.type.SqlTypes#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<char[]> CHAR_ARRAY = new BasicTypeReference<>(
			"characters",
			char[].class,
			SqlTypes.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC
	 * {@link org.hibernate.type.SqlTypes#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<Character[]> CHARACTER_ARRAY = new BasicTypeReference<>(
			"wrapper-characters",
			Character[].class,
			SqlTypes.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link org.hibernate.type.SqlTypes#LONGVARCHAR LONGVARCHAR}.
	 * <p>
	 * Similar to a {@link #MATERIALIZED_CLOB}
	 */
	public static final BasicTypeReference<String> TEXT = new BasicTypeReference<>(
			"text",
			String.class,
			SqlTypes.LONGVARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link org.hibernate.type.SqlTypes#LONGNVARCHAR LONGNVARCHAR}.
	 * <p>
	 * Similar to a {@link #MATERIALIZED_NCLOB}
	 */
	public static final BasicTypeReference<String> NTEXT = new BasicTypeReference<>(
			"ntext",
			String.class,
			SqlTypes.LONGNVARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link Clob} to JDBC {@link org.hibernate.type.SqlTypes#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 */
	public static final BasicTypeReference<Clob> CLOB = new BasicTypeReference<>(
			"clob",
			Clob.class,
			SqlTypes.CLOB
	);

	/**
	 * The standard Hibernate type for mapping {@link NClob} to JDBC {@link org.hibernate.type.SqlTypes#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_NCLOB
	 */
	public static final BasicTypeReference<NClob> NCLOB = new BasicTypeReference<>(
			"nclob",
			NClob.class,
			SqlTypes.NCLOB
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link org.hibernate.type.SqlTypes#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final BasicTypeReference<String> MATERIALIZED_CLOB = new BasicTypeReference<>(
			"materialized_clob",
			String.class,
			SqlTypes.CLOB
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link org.hibernate.type.SqlTypes#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #NTEXT
	 */
	public static final BasicTypeReference<String> MATERIALIZED_NCLOB = new BasicTypeReference<>(
			"materialized_nclob",
			String.class,
			SqlTypes.NCLOB
	);


	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link org.hibernate.type.SqlTypes#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final BasicTypeReference<char[]> MATERIALIZED_CLOB_CHAR_ARRAY = new BasicTypeReference<>(
			"materialized_clob_char_array",
			char[].class,
			SqlTypes.CLOB
	);


	/**
	 * The standard Hibernate type for mapping {@code Character[]} to JDBC {@link org.hibernate.type.SqlTypes#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final BasicTypeReference<Character[]> MATERIALIZED_CLOB_CHARACTER_ARRAY = new BasicTypeReference<>(
			"materialized_clob_character_array",
			Character[].class,
			SqlTypes.CLOB
	);


	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link org.hibernate.type.SqlTypes#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_NCLOB
	 * @see #TEXT
	 */
	public static final BasicTypeReference<char[]> MATERIALIZED_NCLOB_CHAR_ARRAY = new BasicTypeReference<>(
			"materialized_nclob_char_array",
			char[].class,
			SqlTypes.NCLOB
	);


	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC {@link org.hibernate.type.SqlTypes#NCLOB NCLOB} and
	 *
	 * @see #NCLOB
	 * @see #CHAR_ARRAY
	 */
	public static final BasicTypeReference<Character[]> MATERIALIZED_NCLOB_CHARACTER_ARRAY = new BasicTypeReference<>(
			"materialized_nclob_character_array",
			Character[].class,
			SqlTypes.NCLOB
	);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Date / time data

	/**
	 * The standard Hibernate type for mapping {@link Duration} to JDBC {@link org.hibernate.type.SqlTypes#INTERVAL_SECOND INTERVAL_SECOND}
	 * or {@link org.hibernate.type.SqlTypes#NUMERIC NUMERIC} as a fallback.
	 */
	public static final BasicTypeReference<Duration> DURATION = new BasicTypeReference<>(
			"Duration",
			Duration.class,
			SqlTypes.INTERVAL_SECOND
	);

	/**
	 * The standard Hibernate type for mapping {@link LocalDateTime} to JDBC {@link org.hibernate.type.SqlTypes#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicTypeReference<LocalDateTime> LOCAL_DATE_TIME = new BasicTypeReference<>(
			"LocalDateTime",
			LocalDateTime.class,
			SqlTypes.TIMESTAMP
	);

	/**
	 * The standard Hibernate type for mapping {@link LocalDate} to JDBC {@link org.hibernate.type.SqlTypes#DATE DATE}.
	 */
	public static final BasicTypeReference<LocalDate> LOCAL_DATE = new BasicTypeReference<>(
			"LocalDate",
			LocalDate.class,
			SqlTypes.DATE
	);

	/**
	 * The standard Hibernate type for mapping {@link LocalTime} to JDBC {@link org.hibernate.type.SqlTypes#TIME TIME}.
	 */
	public static final BasicTypeReference<LocalTime> LOCAL_TIME = new BasicTypeReference<>(
			"LocalTime",
			LocalTime.class,
			SqlTypes.TIME
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetDateTime} to JDBC {@link org.hibernate.type.SqlTypes#TIMESTAMP_WITH_TIMEZONE TIMESTAMP_WITH_TIMEZONE}
	 * or {@link org.hibernate.type.SqlTypes#TIMESTAMP TIMESTAMP} depending on the {@value org.hibernate.cfg.AvailableSettings#TIMEZONE_DEFAULT_STORAGE} setting.
	 */
	public static final BasicTypeReference<OffsetDateTime> OFFSET_DATE_TIME = new BasicTypeReference<>(
			"OffsetDateTime",
			OffsetDateTime.class,
			SqlTypes.TIMESTAMP_WITH_TIMEZONE
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetDateTime} to JDBC {@link org.hibernate.type.SqlTypes#TIMESTAMP_WITH_TIMEZONE TIMESTAMP_WITH_TIMEZONE}.
	 * This maps to {@link TimeZoneStorageStrategy#NATIVE}.
	 */
	public static final BasicTypeReference<OffsetDateTime> OFFSET_DATE_TIME_WITH_TIMEZONE = new BasicTypeReference<>(
			"OffsetDateTimeWithTimezone",
			OffsetDateTime.class,
			SqlTypes.TIMESTAMP_WITH_TIMEZONE
	);
	/**
	 * The standard Hibernate type for mapping {@link OffsetDateTime} to JDBC {@link org.hibernate.type.SqlTypes#TIMESTAMP TIMESTAMP}.
	 * This maps to {@link TimeZoneStorageStrategy#NORMALIZE}.
	 */
	public static final BasicTypeReference<OffsetDateTime> OFFSET_DATE_TIME_WITHOUT_TIMEZONE = new BasicTypeReference<>(
			"OffsetDateTimeWithoutTimezone",
			OffsetDateTime.class,
			SqlTypes.TIMESTAMP
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetTime} to JDBC {@link org.hibernate.type.SqlTypes#TIME_WITH_TIMEZONE TIME_WITH_TIMEZONE}.
	 */
	public static final BasicTypeReference<OffsetTime> OFFSET_TIME = new BasicTypeReference<>(
			"OffsetTime",
			OffsetTime.class,
			SqlTypes.TIME_WITH_TIMEZONE
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetTime} to JDBC {@link org.hibernate.type.SqlTypes#TIME_UTC TIME_UTC}.
	 * This maps to {@link TimeZoneStorageStrategy#NORMALIZE_UTC}.
	 */
	public static final BasicTypeReference<OffsetTime> OFFSET_TIME_UTC = new BasicTypeReference<>(
			"OffsetTimeUtc",
			OffsetTime.class,
			SqlTypes.TIME_UTC
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetTime} to JDBC {@link org.hibernate.type.SqlTypes#TIME_WITH_TIMEZONE TIME_WITH_TIMEZONE}.
	 * This maps to {@link TimeZoneStorageStrategy#NATIVE}.
	 */
	public static final BasicTypeReference<OffsetTime> OFFSET_TIME_WITH_TIMEZONE = new BasicTypeReference<>(
			"OffsetTimeWithTimezone",
			OffsetTime.class,
			SqlTypes.TIME_WITH_TIMEZONE
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetTime} to JDBC {@link org.hibernate.type.SqlTypes#TIME TIME}.
	 */
	public static final BasicTypeReference<OffsetTime> OFFSET_TIME_WITHOUT_TIMEZONE = new BasicTypeReference<>(
			"OffsetTimeWithoutTimezone",
			OffsetTime.class,
			SqlTypes.TIME
	);

	/**
	 * The standard Hibernate type for mapping {@link ZonedDateTime} to JDBC {@link org.hibernate.type.SqlTypes#TIMESTAMP_WITH_TIMEZONE TIMESTAMP_WITH_TIMEZONE}
	 * or {@link org.hibernate.type.SqlTypes#TIMESTAMP TIMESTAMP} depending on the {@value org.hibernate.cfg.AvailableSettings#TIMEZONE_DEFAULT_STORAGE} setting.
	 */
	public static final BasicTypeReference<ZonedDateTime> ZONED_DATE_TIME = new BasicTypeReference<>(
			"ZonedDateTime",
			ZonedDateTime.class,
			SqlTypes.TIMESTAMP_WITH_TIMEZONE
	);

	/**
	 * The standard Hibernate type for mapping {@link ZonedDateTime} to JDBC {@link org.hibernate.type.SqlTypes#TIMESTAMP_WITH_TIMEZONE TIMESTAMP_WITH_TIMEZONE}.
	 * This maps to {@link TimeZoneStorageStrategy#NATIVE}.
	 */
	public static final BasicTypeReference<ZonedDateTime> ZONED_DATE_TIME_WITH_TIMEZONE = new BasicTypeReference<>(
			"ZonedDateTimeWithTimezone",
			ZonedDateTime.class,
			SqlTypes.TIMESTAMP_WITH_TIMEZONE
	);

	/**
	 * The standard Hibernate type for mapping {@link ZonedDateTime} to JDBC {@link org.hibernate.type.SqlTypes#TIMESTAMP TIMESTAMP}.
	 * This maps to {@link TimeZoneStorageStrategy#NORMALIZE}.
	 */
	public static final BasicTypeReference<ZonedDateTime> ZONED_DATE_TIME_WITHOUT_TIMEZONE = new BasicTypeReference<>(
			"ZonedDateTimeWithoutTimezone",
			ZonedDateTime.class,
			SqlTypes.TIMESTAMP
	);

	/**
	 * The standard Hibernate type for mapping {@link Instant} to JDBC
	 * {@link org.hibernate.type.SqlTypes#TIMESTAMP_UTC TIMESTAMP_UTC}.
	 */
	public static final BasicTypeReference<Instant> INSTANT = new BasicTypeReference<>(
			"instant",
			Instant.class,
			SqlTypes.TIMESTAMP_UTC
	);

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Time}) to JDBC
	 * {@link org.hibernate.type.SqlTypes#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicTypeReference<Date> TIME = new BasicTypeReference<>(
			"time",
			Time.class,
			SqlTypes.TIME
	);

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Date}) to JDBC
	 * {@link org.hibernate.type.SqlTypes#DATE DATE}.
	 */
	public static final BasicTypeReference<Date> DATE = new BasicTypeReference<>(
			"date",
			java.sql.Date.class,
			SqlTypes.DATE
	);

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Timestamp}) to JDBC
	 * {@link org.hibernate.type.SqlTypes#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicTypeReference<Date> TIMESTAMP = new BasicTypeReference<>(
			"timestamp",
			Timestamp.class,
			SqlTypes.TIMESTAMP
	);

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link org.hibernate.type.SqlTypes#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicTypeReference<Calendar> CALENDAR = new BasicTypeReference<>(
			"calendar",
			Calendar.class,
			SqlTypes.TIMESTAMP
	);

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link org.hibernate.type.SqlTypes#DATE DATE}.
	 */
	public static final BasicTypeReference<Calendar> CALENDAR_DATE = new BasicTypeReference<>(
			"calendar_date",
			Calendar.class,
			SqlTypes.DATE
	);

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link org.hibernate.type.SqlTypes#TIME TIME}.
	 */
	public static final BasicTypeReference<Calendar> CALENDAR_TIME = new BasicTypeReference<>(
			"calendar_time",
			Calendar.class,
			SqlTypes.TIME
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Binary mappings

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link org.hibernate.type.SqlTypes#VARBINARY VARBINARY}.
	 */
	public static final BasicTypeReference<byte[]> BINARY = new BasicTypeReference<>(
			"binary",
			byte[].class,
			SqlTypes.VARBINARY
	);

	/**
	 * The standard Hibernate type for mapping {@link Byte Byte[]} to JDBC {@link org.hibernate.type.SqlTypes#VARBINARY VARBINARY}.
	 */
	public static final BasicTypeReference<Byte[]> BINARY_WRAPPER = new BasicTypeReference<>(
			"binary_wrapper",
			Byte[].class,
			SqlTypes.VARBINARY
	);

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link org.hibernate.type.SqlTypes#LONGVARBINARY LONGVARBINARY}.
	 *
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BasicTypeReference<byte[]> IMAGE = new BasicTypeReference<>(
			"image",
			byte[].class,
			SqlTypes.LONGVARBINARY
	);

	/**
	 * The standard Hibernate type for mapping {@link Blob} to JDBC {@link org.hibernate.type.SqlTypes#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BasicTypeReference<Blob> BLOB = new BasicTypeReference<>(
			"blob",
			Blob.class,
			SqlTypes.BLOB
	);

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link org.hibernate.type.SqlTypes#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final BasicTypeReference<byte[]> MATERIALIZED_BLOB = new BasicTypeReference<>(
			"materialized_blob",
			byte[].class,
			SqlTypes.BLOB
	);

	/**
	 * The standard Hibernate type for mapping {@code Byte[]} to JDBC {@link org.hibernate.type.SqlTypes#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final BasicTypeReference<Byte[]> MATERIALIZED_BLOB_WRAPPER = new BasicTypeReference<>(
			"materialized_blob_wrapper",
			Byte[].class,
			SqlTypes.BLOB
	);

	/**
	 * The standard Hibernate type for mapping {@link Serializable} to JDBC {@link org.hibernate.type.SqlTypes#VARBINARY VARBINARY}.
	 * <p>
	 * See especially the discussion wrt {@link ClassLoader} determination on {@link SerializableType}
	 */
	public static final BasicTypeReference<Serializable> SERIALIZABLE = new BasicTypeReference<>(
			"serializable",
			Serializable.class,
			SqlTypes.VARBINARY
	);

	public static final BasicTypeReference<Object> OBJECT_TYPE = new BasicTypeReference<>(
			"JAVA_OBJECT",
			Object.class,
			SqlTypes.JAVA_OBJECT
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc JDK types

	/**
	 * The standard Hibernate type for mapping {@link Class} to JDBC {@link org.hibernate.type.SqlTypes#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<Class> CLASS = new BasicTypeReference<>(
			"class",
			Class.class,
			SqlTypes.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link Locale} to JDBC {@link org.hibernate.type.SqlTypes#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<Locale> LOCALE = new BasicTypeReference<>(
			"locale",
			Locale.class,
			SqlTypes.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link Currency} to JDBC {@link org.hibernate.type.SqlTypes#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<Currency> CURRENCY = new BasicTypeReference<>(
			"currency",
			Currency.class,
			SqlTypes.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link ZoneOffset} to JDBC {@link org.hibernate.type.SqlTypes#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<ZoneOffset> ZONE_OFFSET = new BasicTypeReference<>(
			"ZoneOffset",
			ZoneOffset.class,
			SqlTypes.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link TimeZone} to JDBC {@link org.hibernate.type.SqlTypes#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<TimeZone> TIMEZONE = new BasicTypeReference<>(
			"timezone",
			TimeZone.class,
			SqlTypes.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link java.net.URL} to JDBC {@link org.hibernate.type.SqlTypes#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<URL> URL = new BasicTypeReference<>(
			"url",
			java.net.URL.class,
			SqlTypes.VARCHAR
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UUID mappings

	/**
	 * The standard Hibernate type for mapping {@link UUID} to JDBC {@link org.hibernate.type.SqlTypes#UUID UUID}
	 * which maps to a native UUID SQL type if possible and falls back to {@link org.hibernate.type.SqlTypes#BINARY BINARY}.
	 */
	public static final BasicTypeReference<UUID> UUID = new BasicTypeReference<>(
			"uuid",
			UUID.class,
			SqlTypes.UUID
	);

	/**
	 * The standard Hibernate type for mapping {@link UUID} to JDBC {@link org.hibernate.type.SqlTypes#BINARY BINARY}.
	 */
	public static final BasicTypeReference<UUID> UUID_BINARY = new BasicTypeReference<>(
			"uuid-binary",
			UUID.class,
			SqlTypes.BINARY
	);

	/**
	 * The standard Hibernate type for mapping {@link UUID} to JDBC {@link org.hibernate.type.SqlTypes#CHAR CHAR}.
	 */
	public static final BasicTypeReference<UUID> UUID_CHAR = new BasicTypeReference<>(
			"uuid-char",
			UUID.class,
			SqlTypes.CHAR
	);


	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link org.hibernate.type.SqlTypes#VARBINARY VARBINARY},
	 * specifically for entity versions/timestamps.  Only useful for T-SQL databases (MS, Sybase, etc)
	 */
	public static final BasicTypeReference<byte[]> ROW_VERSION = new BasicTypeReference<>(
			"row_version", byte[].class, SqlTypes.VARBINARY
	);


	/**
	 * The standard Hibernate type for mapping {@code float[]} to JDBC {@link org.hibernate.type.SqlTypes#VECTOR VECTOR},
	 * specifically for embedding vectors like provided by the PostgreSQL extension pgvector and Oracle 23ai.
	 */
	public static final BasicTypeReference<float[]> VECTOR = new BasicTypeReference<>(
			"vector", float[].class, SqlTypes.VECTOR
	);

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link org.hibernate.type.SqlTypes#VECTOR_INT8 VECTOR_INT8},
	 * specifically for embedding integer vectors (8-bits) like provided by Oracle 23ai.
	 */
	public static final BasicTypeReference<byte[]> VECTOR_INT8 = new BasicTypeReference<>(
			"byte_vector", byte[].class, SqlTypes.VECTOR_INT8
	);

	/**
	 * The standard Hibernate type for mapping {@code float[]} to JDBC {@link org.hibernate.type.SqlTypes#VECTOR_FLOAT16 VECTOR_FLOAT16},
	 * specifically for embedding half-precision floating-point (16-bits) vectors like provided by the PostgreSQL extension pgvector.
	 */
	public static final BasicTypeReference<float[]> VECTOR_FLOAT16 = new BasicTypeReference<>(
			"float16_vector", float[].class, SqlTypes.VECTOR_FLOAT16
	);

	/**
	 * The standard Hibernate type for mapping {@code float[]} to JDBC {@link org.hibernate.type.SqlTypes#VECTOR VECTOR},
	 * specifically for embedding single-precision floating-point (32-bits) vectors like provided by Oracle 23ai.
	 */
	public static final BasicTypeReference<float[]> VECTOR_FLOAT32 = new BasicTypeReference<>(
			"float_vector", float[].class, SqlTypes.VECTOR_FLOAT32
	);

	/**
	 * The standard Hibernate type for mapping {@code double[]} to JDBC {@link org.hibernate.type.SqlTypes#VECTOR VECTOR},
	 * specifically for embedding double-precision floating-point (64-bits) vectors like provided by Oracle 23ai.
	 */
	public static final BasicTypeReference<double[]> VECTOR_FLOAT64 = new BasicTypeReference<>(
			"double_vector", double[].class, SqlTypes.VECTOR_FLOAT64
	);

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link org.hibernate.type.SqlTypes#VECTOR_BINARY VECTOR_BIT},
	 * specifically for embedding bit vectors like provided by Oracle 23ai.
	 */
	public static final BasicTypeReference<byte[]> VECTOR_BINARY = new BasicTypeReference<>(
			"binary_vector", byte[].class, SqlTypes.VECTOR_BINARY
	);

//	/**
//	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link org.hibernate.type.SqlTypes#VECTOR_INT8 VECTOR_INT8},
//	 * specifically for embedding integer vectors (8-bits) like provided by Oracle 23ai.
//	 */
//	public static final BasicTypeReference<byte[]> SPARSE_VECTOR_INT8 = new BasicTypeReference<>(
//			"sparse_byte_vector", byte[].class, SqlTypes.SPARSE_VECTOR_INT8
//	);
//
//	/**
//	 * The standard Hibernate type for mapping {@code float[]} to JDBC {@link org.hibernate.type.SqlTypes#VECTOR VECTOR},
//	 * specifically for embedding single-precision floating-point (32-bits) vectors like provided by Oracle 23ai.
//	 */
//	public static final BasicTypeReference<float[]> SPARSE_VECTOR_FLOAT32 = new BasicTypeReference<>(
//			"sparse_float_vector", float[].class, SqlTypes.SPARSE_VECTOR_FLOAT32
//	);
//
//	/**
//	 * The standard Hibernate type for mapping {@code double[]} to JDBC {@link org.hibernate.type.SqlTypes#VECTOR VECTOR},
//	 * specifically for embedding double-precision floating-point (64-bits) vectors like provided by Oracle 23ai.
//	 */
//	public static final BasicTypeReference<double[]> SPARSE_VECTOR_FLOAT64 = new BasicTypeReference<>(
//			"sparse_double_vector", double[].class, SqlTypes.SPARSE_VECTOR_FLOAT64
//	);


	public static void prime(TypeConfiguration typeConfiguration) {
		BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

		if ( basicTypeRegistry.isPrimed() ) {
			return;
		}

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// boolean data


		handle(
				BOOLEAN,
				"org.hibernate.type.BooleanType",
				basicTypeRegistry,
				"boolean", boolean.class.getName(), Boolean.class.getName()
		);

		handle(
				NUMERIC_BOOLEAN,
				"org.hibernate.type.NumericBooleanType",
				basicTypeRegistry,
				"numeric_boolean", NumericBooleanConverter.class.getName()
		);

		handle(
				TRUE_FALSE,
				"org.hibernate.type.TrueFalseType",
				basicTypeRegistry,
				"true_false", TrueFalseConverter.class.getName()
		);

		handle(
				YES_NO,
				"org.hibernate.type.YesNoType",
				basicTypeRegistry,
				"yes_no", YesNoConverter.class.getName()
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// byte/binary data

		handle(
				BYTE,
				"org.hibernate.type.ByteType",
				basicTypeRegistry,
				"byte", byte.class.getName(), Byte.class.getName()
		);

		handle(
				BINARY,
				"org.hibernate.type.BinaryType",
				basicTypeRegistry,
				"binary", "byte[]", byte[].class.getName()
		);

		handle(
				BINARY_WRAPPER,
				"org.hibernate.type.WrapperBinaryType",
				basicTypeRegistry,
				"binary_wrapper", "wrapper-binary"//, "Byte[]", Byte[].class.getName()
		);

		handle(
				IMAGE,
				"org.hibernate.type.ImageType",
				basicTypeRegistry,
				"image"
		);

		handle(
				BLOB,
				"org.hibernate.type.BlobType",
				basicTypeRegistry,
				"blob",
				Blob.class.getName()
		);

		handle(
				MATERIALIZED_BLOB,
				"org.hibernate.type.MaterializedBlobType",
				basicTypeRegistry,
				"materialized_blob"
		);

		handle(
				MATERIALIZED_BLOB_WRAPPER,
				"org.hibernate.type.WrappedMaterializedBlobType",
				basicTypeRegistry,
				"materialized_blob_wrapper"
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Numeric data

		handle(
				SHORT,
				"org.hibernate.type.ShortType",
				basicTypeRegistry,
				"short", short.class.getName(), Short.class.getName()
		);

		handle(
				INTEGER,
				"org.hibernate.type.IntegerType",
				basicTypeRegistry,
				"integer", int.class.getName(), Integer.class.getName()
		);

		handle(
				LONG,
				"org.hibernate.type.LongType",
				basicTypeRegistry,
				"long", long.class.getName(), Long.class.getName()
		);

		handle(
				FLOAT,
				"org.hibernate.type.FloatType",
				basicTypeRegistry,
				"float", float.class.getName(), Float.class.getName()
		);

		handle(
				DOUBLE,
				"org.hibernate.type.DoubleType",
				basicTypeRegistry,
				"double", double.class.getName(), Double.class.getName()
		);

		handle(
				BIG_INTEGER,
				"org.hibernate.type.BigIntegerType",
				basicTypeRegistry,
				"big_integer", BigInteger.class.getName()
		);

		handle(
				BIG_DECIMAL,
				"org.hibernate.type.BigDecimalType",
				basicTypeRegistry,
				"big_decimal", BigDecimal.class.getName()
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// String data

		handle(
				CHARACTER,
				"org.hibernate.type.CharacterType",
				basicTypeRegistry,
				"character", char.class.getName(), Character.class.getName()
		);

		handle(
				CHARACTER_NCHAR,
				null,
				basicTypeRegistry,
				"character_nchar"
		);

		handle(
				STRING,
				"org.hibernate.type.StringType",
				basicTypeRegistry,
				"string", String.class.getName()
		);

		handle(
				NSTRING,
				"org.hibernate.type.StringNVarcharType",
				basicTypeRegistry,
				"nstring"
		);

		handle(
				CHAR_ARRAY,
				"org.hibernate.type.CharArrayType",
				basicTypeRegistry,
				"characters", "char[]", char[].class.getName()
		);

		handle(
				CHARACTER_ARRAY,
				"org.hibernate.type.CharacterArrayType",
				basicTypeRegistry,
				"wrapper-characters"//, Character[].class.getName(), "Character[]"
		);

		handle(
				TEXT,
				"org.hibernate.type.TextType",
				basicTypeRegistry,
				"text"
		);

		handle(
				NTEXT,
				"org.hibernate.type.NTextType",
				basicTypeRegistry,
				"ntext"
		);

		handle(
				CLOB,
				"org.hibernate.type.ClobType",
				basicTypeRegistry,
				"clob", Clob.class.getName()
		);

		handle(
				NCLOB,
				"org.hibernate.type.NClobType",
				basicTypeRegistry,
				"nclob", NClob.class.getName()
		);

		handle(
				MATERIALIZED_CLOB,
				"org.hibernate.type.MaterializedClobType",
				basicTypeRegistry,
				"materialized_clob"
		);

		handle(
				MATERIALIZED_CLOB_CHAR_ARRAY,
				"org.hibernate.type.PrimitiveCharacterArrayClobType",
				basicTypeRegistry,
				"materialized_clob_char_array"
		);

		handle(
				MATERIALIZED_CLOB_CHARACTER_ARRAY,
				"org.hibernate.type.CharacterArrayClobType",
				basicTypeRegistry,
				"materialized_clob_character_array"
		);

		handle(
				MATERIALIZED_NCLOB,
				"org.hibernate.type.MaterializedNClobType",
				basicTypeRegistry,
				"materialized_nclob"
		);

		handle(
				MATERIALIZED_NCLOB_CHARACTER_ARRAY,
				"org.hibernate.type.CharacterArrayNClobType",
				basicTypeRegistry,
				"materialized_nclob_character_array"
		);

		handle(
				MATERIALIZED_NCLOB_CHAR_ARRAY,
				"org.hibernate.type.PrimitiveCharacterArrayNClobType",
				basicTypeRegistry,
				"materialized_nclob_char_array"
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// date / time data

		handle(
				DURATION,
				"org.hibernate.type.DurationType",
				basicTypeRegistry,
				Duration.class.getSimpleName(), Duration.class.getName()
		);

		handle(
				LOCAL_DATE_TIME,
				"org.hibernate.type.LocalDateTimeType",
				basicTypeRegistry,
				LocalDateTime.class.getSimpleName(), LocalDateTime.class.getName()
		);

		handle(
				LOCAL_DATE,
				"org.hibernate.type.LocalDateType",
				basicTypeRegistry,
				LocalDate.class.getSimpleName(), LocalDate.class.getName()
		);

		handle(
				LOCAL_TIME,
				"org.hibernate.type.LocalTimeType",
				basicTypeRegistry,
				LocalTime.class.getSimpleName(), LocalTime.class.getName()
		);

		handle(
				OFFSET_DATE_TIME,
				"org.hibernate.type.OffsetDateTimeType",
				basicTypeRegistry,
				OffsetDateTime.class.getSimpleName(), OffsetDateTime.class.getName()
		);

		handle(
				OFFSET_DATE_TIME_WITH_TIMEZONE,
				null,
				basicTypeRegistry,
				OFFSET_DATE_TIME_WITH_TIMEZONE.getName()
		);

		handle(
				OFFSET_DATE_TIME_WITHOUT_TIMEZONE,
				null,
				basicTypeRegistry,
				OFFSET_DATE_TIME_WITHOUT_TIMEZONE.getName()
		);

		handle(
				OFFSET_TIME,
				"org.hibernate.type.OffsetTimeType",
				basicTypeRegistry,
				OffsetTime.class.getSimpleName(), OffsetTime.class.getName()
		);

		handle(
				OFFSET_TIME_UTC,
				null,
				basicTypeRegistry,
				OFFSET_TIME_UTC.getName()
		);

		handle(
				OFFSET_TIME_WITH_TIMEZONE,
				null,
				basicTypeRegistry,
				OFFSET_TIME_WITH_TIMEZONE.getName()
		);

		handle(
				OFFSET_TIME_WITHOUT_TIMEZONE,
				null,
				basicTypeRegistry,
				OFFSET_TIME_WITHOUT_TIMEZONE.getName()
		);

		handle(
				ZONED_DATE_TIME,
				"org.hibernate.type.ZonedDateTimeType",
				basicTypeRegistry,
				ZonedDateTime.class.getSimpleName(), ZonedDateTime.class.getName()
		);

		handle(
				ZONED_DATE_TIME_WITH_TIMEZONE,
				null,
				basicTypeRegistry,
				ZONED_DATE_TIME_WITH_TIMEZONE.getName()
		);

		handle(
				ZONED_DATE_TIME_WITHOUT_TIMEZONE,
				null,
				basicTypeRegistry,
				ZONED_DATE_TIME_WITHOUT_TIMEZONE.getName()
		);

		handle(
				DATE,
				"org.hibernate.type.DateType",
				basicTypeRegistry,
				"date", java.sql.Date.class.getName()
		);

		handle(
				TIME,
				"org.hibernate.type.TimeType",
				basicTypeRegistry,
				"time", java.sql.Time.class.getName()
		);

		handle(
				TIMESTAMP,
				"org.hibernate.type.TimestampType",
				basicTypeRegistry,
				"timestamp", java.sql.Timestamp.class.getName(), Date.class.getName()
		);

		handle(
				CALENDAR,
				"org.hibernate.type.CalendarType",
				basicTypeRegistry,
				"calendar", Calendar.class.getName(), GregorianCalendar.class.getName()
		);

		handle(
				CALENDAR_DATE,
				"org.hibernate.type.CalendarDateType",
				basicTypeRegistry,
				"calendar_date"
		);

		handle(
				CALENDAR_TIME,
				"org.hibernate.type.CalendarTimeType",
				basicTypeRegistry,
				"calendar_time"
		);

		handle(
				INSTANT,
				"org.hibernate.type.InstantType",
				basicTypeRegistry,
				"instant", Instant.class.getName()
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// UUID data

		handle(
				UUID,
				"org.hibernate.type.PostgresUUIDType",
				basicTypeRegistry,
				"uuid", UUID.class.getName(), "pg-uuid"
		);

		handle(
				UUID_BINARY,
				"org.hibernate.type.UUIDBinaryType",
				basicTypeRegistry,
				"uuid-binary"
		);

		handle(
				UUID_CHAR,
				"org.hibernate.type.UUIDCharType",
				basicTypeRegistry,
				"uuid-char"
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Misc data

		handle(
				CLASS,
				"org.hibernate.type.ClassType",
				basicTypeRegistry,
				"class", Class.class.getName()
		);

		handle(
				CURRENCY,
				"org.hibernate.type.CurrencyType",
				basicTypeRegistry,
				"currency", Currency.class.getSimpleName(), Currency.class.getName()
		);

		handle(
				LOCALE,
				"org.hibernate.type.LocaleType",
				basicTypeRegistry,
				"locale",
				Locale.class.getName()
		);

		handle(
				SERIALIZABLE,
				"org.hibernate.type.SerializableType",
				basicTypeRegistry,
				"serializable", Serializable.class.getName()
		);

		handle(
				TIMEZONE,
				"org.hibernate.type.TimeZoneType",
				basicTypeRegistry,
				"timezone", TimeZone.class.getName()
		);

		handle(
				ZONE_OFFSET,
				"org.hibernate.type.ZoneOffsetType",
				basicTypeRegistry,
				ZoneOffset.class.getSimpleName(), ZoneOffset.class.getName()
		);

		handle(
				URL,
				"org.hibernate.type.UrlType",
				basicTypeRegistry,
				"url", java.net.URL.class.getName()
		);

		handle(
				VECTOR,
				null,
				basicTypeRegistry,
				"vector"
		);

		handle(
				VECTOR_FLOAT32,
				null,
				basicTypeRegistry,
				"float_vector"
		);

		handle(
				VECTOR_FLOAT64,
				null,
				basicTypeRegistry,
				"double_vector"
		);

		handle(
				VECTOR_INT8,
				null,
				basicTypeRegistry,
				"byte_vector"
		);

		handle(
				VECTOR_BINARY,
				null,
				basicTypeRegistry,
				"bit_vector"
		);

//		handle(
//				SPARSE_VECTOR_FLOAT32,
//				null,
//				basicTypeRegistry,
//				"sparse_float_vector"
//		);
//
//		handle(
//				SPARSE_VECTOR_FLOAT64,
//				null,
//				basicTypeRegistry,
//				"sparse_double_vector"
//		);
//
//		handle(
//				SPARSE_VECTOR_INT8,
//				null,
//				basicTypeRegistry,
//				"sparse_byte_vector"
//		);


		// Specialized version handlers

		handle(
				ROW_VERSION,
				null,
				basicTypeRegistry,
				"row_version"
		);

		handle(
				JavaObjectType.INSTANCE,
				null,
				basicTypeRegistry,
				"object", Object.class.getName()
		);

		handle(
				NullType.INSTANCE,
				null,
				basicTypeRegistry,
				"null"
		);

		final BasicTypeReference<Date> dateTypeImmutableType = DATE.asImmutable();
		handle( dateTypeImmutableType, null, basicTypeRegistry, dateTypeImmutableType.getName() );

		final BasicTypeReference<Date> timeTypeImmutableType = TIME.asImmutable();
		handle( timeTypeImmutableType, null, basicTypeRegistry, timeTypeImmutableType.getName() );

		final BasicTypeReference<Date> timeStampImmutableType = TIMESTAMP.asImmutable();
		handle( timeStampImmutableType, null, basicTypeRegistry, timeStampImmutableType.getName() );

		final BasicTypeReference<Calendar> calendarImmutableType = CALENDAR.asImmutable();
		handle( calendarImmutableType, null, basicTypeRegistry, calendarImmutableType.getName() );

		final BasicTypeReference<Calendar> calendarDateImmutableType = CALENDAR_DATE.asImmutable();
		handle( calendarDateImmutableType, null, basicTypeRegistry, calendarDateImmutableType.getName() );

		final BasicTypeReference<Calendar> calendarTimeImmutableType = CALENDAR_TIME.asImmutable();
		handle( calendarTimeImmutableType, null, basicTypeRegistry, calendarTimeImmutableType.getName() );

		final BasicTypeReference<byte[]> binaryImmutableType = BINARY.asImmutable();
		handle( binaryImmutableType, null, basicTypeRegistry, binaryImmutableType.getName() );

		final BasicTypeReference<Serializable> serializableImmutableType = SERIALIZABLE.asImmutable();
		handle( serializableImmutableType, null, basicTypeRegistry, serializableImmutableType.getName() );

		basicTypeRegistry.primed();
	}

	private static void handle(
			BasicType<?> type,
			String legacyTypeClassName,
			BasicTypeRegistry basicTypeRegistry,
			String... registrationKeys) {
		basicTypeRegistry.addPrimeEntry( type, legacyTypeClassName, registrationKeys );
	}

	private static void handle(
			BasicTypeReference<?> type,
			String legacyTypeClassName,
			BasicTypeRegistry basicTypeRegistry,
			String... registrationKeys) {
		basicTypeRegistry.addPrimeEntry( type, legacyTypeClassName, registrationKeys );
	}

}
