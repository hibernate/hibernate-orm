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
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
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
 * Centralizes access to the standard set of basic {@link Type types}.
 * <p/>
 * Type mappings can be adjusted per {@link org.hibernate.SessionFactory}.
 *
 * @see BasicTypeRegistry
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
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#BOOLEAN BOOLEAN}.
	 */
	public static final BasicTypeReference<Boolean> BOOLEAN = new BasicTypeReference<>(
			"boolean",
			Boolean.class,
			Types.BOOLEAN
	);

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#TINYINT TINYINT}.
	 */
	public static final BasicTypeReference<Boolean> NUMERIC_BOOLEAN = new BasicTypeReference<>(
			"numeric_boolean",
			Boolean.class,
			Types.TINYINT,
			NumericBooleanConverter.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'T'/'F').
	 */
	public static final BasicTypeReference<Boolean> TRUE_FALSE = new BasicTypeReference<>(
			"true_false",
			Boolean.class,
			Types.CHAR,
			TrueFalseConverter.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'Y'/'N').
	 */
	public static final BasicTypeReference<Boolean> YES_NO = new BasicTypeReference<>(
			"yes_no",
			Boolean.class,
			Types.CHAR,
			YesNoConverter.INSTANCE
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Numeric mappings

	/**
	 * The standard Hibernate type for mapping {@link Byte} to JDBC {@link java.sql.Types#TINYINT TINYINT}.
	 */
	public static final BasicTypeReference<Byte> BYTE = new BasicTypeReference<>(
			"byte",
			Byte.class,
			Types.TINYINT
	);

	/**
	 * The standard Hibernate type for mapping {@link Short} to JDBC {@link java.sql.Types#SMALLINT SMALLINT}.
	 */
	public static final BasicTypeReference<Short> SHORT = new BasicTypeReference<>(
			"short",
			Short.class,
			Types.SMALLINT
	);

	/**
	 * The standard Hibernate type for mapping {@link Integer} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 */
	public static final BasicTypeReference<Integer> INTEGER = new BasicTypeReference<>(
			"integer",
			Integer.class,
			Types.INTEGER
	);

	/**
	 * The standard Hibernate type for mapping {@link Long} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 */
	public static final BasicTypeReference<Long> LONG = new BasicTypeReference<>(
			"long",
			Long.class,
			Types.BIGINT
	);

	/**
	 * The standard Hibernate type for mapping {@link Float} to JDBC {@link java.sql.Types#FLOAT FLOAT}.
	 */
	public static final BasicTypeReference<Float> FLOAT = new BasicTypeReference<>(
			"float",
			Float.class,
			Types.FLOAT
	);

	/**
	 * The standard Hibernate type for mapping {@link Double} to JDBC {@link java.sql.Types#DOUBLE DOUBLE}.
	 */
	public static final BasicTypeReference<Double> DOUBLE = new BasicTypeReference<>(
			"double",
			Double.class,
			Types.DOUBLE
	);

	/**
	 * The standard Hibernate type for mapping {@link BigInteger} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 */
	public static final BasicTypeReference<BigInteger> BIG_INTEGER = new BasicTypeReference<>(
			"big_integer",
			BigInteger.class,
			Types.NUMERIC
	);

	/**
	 * The standard Hibernate type for mapping {@link BigDecimal} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 */
	public static final BasicTypeReference<BigDecimal> BIG_DECIMAL = new BasicTypeReference<>(
			"big_decimal",
			BigDecimal.class,
			Types.NUMERIC
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Character mappings

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#CHAR CHAR(1)}.
	 */
	public static final BasicTypeReference<Character> CHARACTER = new BasicTypeReference<>(
			"character",
			Character.class,
			Types.CHAR
	);;

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#NCHAR NCHAR(1)}.
	 */
	public static final BasicTypeReference<Character> CHARACTER_NCHAR = new BasicTypeReference<>(
			"ncharacter",
			Character.class,
			Types.NCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<String> STRING = new BasicTypeReference<>(
			"string",
			String.class,
			Types.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NVARCHAR NVARCHAR}
	 */
	public static final BasicTypeReference<String> NSTRING = new BasicTypeReference<>(
			"nstring",
			String.class,
			Types.NVARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<char[]> CHAR_ARRAY = new BasicTypeReference<>(
			"characters",
			char[].class,
			Types.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC
	 * {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<Character[]> CHARACTER_ARRAY = new BasicTypeReference<>(
			"wrapper-characters",
			Character[].class,
			Types.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_CLOB}
	 */
	public static final BasicTypeReference<String> TEXT = new BasicTypeReference<>(
			"text",
			String.class,
			Types.LONGVARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGNVARCHAR LONGNVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_NCLOB}
	 */
	public static final BasicTypeReference<String> NTEXT = new BasicTypeReference<>(
			"ntext",
			String.class,
			Types.LONGNVARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link Clob} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 */
	public static final BasicTypeReference<Clob> CLOB = new BasicTypeReference<>(
			"clob",
			Clob.class,
			Types.CLOB
	);

	/**
	 * The standard Hibernate type for mapping {@link NClob} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_NCLOB
	 */
	public static final BasicTypeReference<NClob> NCLOB = new BasicTypeReference<>(
			"nclob",
			NClob.class,
			Types.NCLOB
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final BasicTypeReference<String> MATERIALIZED_CLOB = new BasicTypeReference<>(
			"materialized_clob",
			String.class,
			Types.CLOB
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #NTEXT
	 */
	public static final BasicTypeReference<String> MATERIALIZED_NCLOB = new BasicTypeReference<>(
			"materialized_nclob",
			String.class,
			Types.NCLOB
	);


	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final BasicTypeReference<char[]> MATERIALIZED_CLOB_CHAR_ARRAY = new BasicTypeReference<>(
			"materialized_clob_char_array",
			char[].class,
			Types.CLOB
	);


	/**
	 * The standard Hibernate type for mapping {@code Character[]} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final BasicTypeReference<Character[]> MATERIALIZED_CLOB_CHARACTER_ARRAY = new BasicTypeReference<>(
			"materialized_clob_character_array",
			Character[].class,
			Types.CLOB
	);


	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_NCLOB
	 * @see #TEXT
	 */
	public static final BasicTypeReference<char[]> MATERIALIZED_NCLOB_CHAR_ARRAY = new BasicTypeReference<>(
			"materialized_nclob_char_array",
			char[].class,
			Types.NCLOB
	);


	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC {@link java.sql.Types#NCLOB NCLOB} and
	 *
	 * @see #NCLOB
	 * @see #CHAR_ARRAY
	 */
	public static final BasicTypeReference<Character[]> MATERIALIZED_NCLOB_CHARACTER_ARRAY = new BasicTypeReference<>(
			"materialized_nclob_character_array",
			Character[].class,
			Types.NCLOB
	);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Date / time data

	/**
	 * The standard Hibernate type for mapping {@link Duration} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 */
	public static final BasicTypeReference<Duration> DURATION = new BasicTypeReference<>(
			"Duration",
			Duration.class,
			Types.NUMERIC
	);

	/**
	 * The standard Hibernate type for mapping {@link LocalDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicTypeReference<LocalDateTime> LOCAL_DATE_TIME = new BasicTypeReference<>(
			"LocalDateTime",
			LocalDateTime.class,
			Types.TIMESTAMP
	);

	/**
	 * The standard Hibernate type for mapping {@link LocalDate} to JDBC {@link java.sql.Types#DATE DATE}.
	 */
	public static final BasicTypeReference<LocalDate> LOCAL_DATE = new BasicTypeReference<>(
			"LocalDate",
			LocalDate.class,
			Types.DATE
	);

	/**
	 * The standard Hibernate type for mapping {@link LocalTime} to JDBC {@link java.sql.Types#TIME TIME}.
	 */
	public static final BasicTypeReference<LocalTime> LOCAL_TIME = new BasicTypeReference<>(
			"LocalTime",
			LocalTime.class,
			Types.TIME
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetDateTime} to JDBC {@link java.sql.Types#TIMESTAMP_WITH_TIMEZONE TIMESTAMP_WITH_TIMEZONE}.
	 */
	public static final BasicTypeReference<OffsetDateTime> OFFSET_DATE_TIME = new BasicTypeReference<>(
			"OffsetDateTime",
			OffsetDateTime.class,
			Types.TIMESTAMP_WITH_TIMEZONE
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetTime} to JDBC {@link java.sql.Types#TIME TIME}.
	 */
	public static final BasicTypeReference<OffsetTime> OFFSET_TIME = new BasicTypeReference<>(
			"ZonedDateTime",
			OffsetTime.class,
			// todo (6.0): why not TIME_WITH_TIMEZONE ?
			Types.TIME
	);

	/**
	 * The standard Hibernate type for mapping {@link ZonedDateTime} to JDBC {@link java.sql.Types#TIMESTAMP_WITH_TIMEZONE TIMESTAMP_WITH_TIMEZONE}.
	 */
	public static final BasicTypeReference<ZonedDateTime> ZONED_DATE_TIME = new BasicTypeReference<>(
			"ZonedDateTime",
			ZonedDateTime.class,
			Types.TIMESTAMP_WITH_TIMEZONE
	);

	/**
	 * The standard Hibernate type for mapping {@link Instant} to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicTypeReference<Instant> INSTANT = new BasicTypeReference<>(
			"instant",
			Instant.class,
			Types.TIMESTAMP
	);

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Time}) to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicTypeReference<Date> TIME = new BasicTypeReference<>(
			"time",
			Time.class,
			Types.TIME
	);

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Date}) to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 */
	public static final BasicTypeReference<Date> DATE = new BasicTypeReference<>(
			"date",
			java.sql.Date.class,
			Types.DATE
	);

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Timestamp}) to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicTypeReference<Date> TIMESTAMP = new BasicTypeReference<>(
			"timestamp",
			Timestamp.class,
			Types.TIMESTAMP
	);

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicTypeReference<Calendar> CALENDAR = new BasicTypeReference<>(
			"calendar",
			Calendar.class,
			Types.TIMESTAMP
	);

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 */
	public static final BasicTypeReference<Calendar> CALENDAR_DATE = new BasicTypeReference<>(
			"calendar_date",
			Calendar.class,
			Types.DATE
	);

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 */
	public static final BasicTypeReference<Calendar> CALENDAR_TIME = new BasicTypeReference<>(
			"calendar_time",
			Calendar.class,
			Types.TIME
	);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Binary mappings

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 */
	public static final BasicTypeReference<byte[]> BINARY = new BasicTypeReference<>(
			"binary",
			byte[].class,
			Types.VARBINARY
	);

	/**
	 * The standard Hibernate type for mapping {@link Byte Byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 */
	public static final BasicTypeReference<Byte[]> WRAPPER_BINARY = new BasicTypeReference<>(
			//TODO find a decent name before documenting
			"wrapper-binary",
			Byte[].class,
			Types.VARBINARY
	);

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY}.
	 *
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BasicTypeReference<byte[]> IMAGE = new BasicTypeReference<>(
			"image",
			byte[].class,
			Types.LONGVARBINARY
	);

	/**
	 * The standard Hibernate type for mapping {@link Blob} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BasicTypeReference<Blob> BLOB = new BasicTypeReference<>(
			"blob",
			Blob.class,
			Types.BLOB
	);

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final BasicTypeReference<byte[]> MATERIALIZED_BLOB = new BasicTypeReference<>(
			"materialized_blob",
			byte[].class,
			Types.BLOB
	);

	/**
	 * The standard Hibernate type for mapping {@code Byte[]} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final BasicTypeReference<Byte[]> WRAPPED_MATERIALIZED_BLOB = new BasicTypeReference<>(
			"wrapped_materialized_blob",
			Byte[].class,
			Types.BLOB
	);

	/**
	 * The standard Hibernate type for mapping {@link Serializable} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 * <p/>
	 * See especially the discussion wrt {@link ClassLoader} determination on {@link SerializableType}
	 */
	public static final BasicTypeReference<Serializable> SERIALIZABLE = new BasicTypeReference<>(
			"serializable",
			Serializable.class,
			Types.VARBINARY
	);

	public static final BasicTypeReference<Object> OBJECT_TYPE = new BasicTypeReference<>(
			"JAVA_OBJECT",
			Object.class,
			Types.JAVA_OBJECT
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc JDK types

	/**
	 * The standard Hibernate type for mapping {@link Class} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<Class> CLASS = new BasicTypeReference<>(
			"class",
			Class.class,
			Types.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link Locale} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<Locale> LOCALE = new BasicTypeReference<>(
			"locale",
			Locale.class,
			Types.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link Currency} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<Currency> CURRENCY = new BasicTypeReference<>(
			"currency",
			Currency.class,
			Types.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link ZoneOffset} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<ZoneOffset> ZONE_OFFSET = new BasicTypeReference<>(
			"ZoneOffset",
			ZoneOffset.class,
			Types.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link TimeZone} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<TimeZone> TIMEZONE = new BasicTypeReference<>(
			"timezone",
			TimeZone.class,
			Types.VARCHAR
	);

	/**
	 * The standard Hibernate type for mapping {@link java.net.URL} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicTypeReference<URL> URL = new BasicTypeReference<>(
			"url",
			java.net.URL.class,
			Types.VARCHAR
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UUID mappings

	/**
	 * The standard Hibernate type for mapping {@link UUID} to JDBC {@link java.sql.Types#BINARY BINARY}.
	 */
	public static final BasicTypeReference<UUID> UUID_BINARY = new BasicTypeReference<>(
			"uuid-binary",
			UUID.class,
			Types.BINARY
	);

	/**
	 * The standard Hibernate type for mapping {@link UUID} to JDBC {@link java.sql.Types#CHAR CHAR}.
	 */
	public static final BasicTypeReference<UUID> UUID_CHAR = new BasicTypeReference<>(
			"uuid-char",
			UUID.class,
			Types.CHAR
	);


	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY},
	 * specifically for entity versions/timestamps.  Only useful for T-SQL databases (MS, Sybase, etc)
	 */
	public static final BasicTypeReference<byte[]> ROW_VERSION = new BasicTypeReference<>(
			"row_version", byte[].class, Types.VARBINARY
	);


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
				"numeric_boolean"
		);

		handle(
				TRUE_FALSE,
				"org.hibernate.type.TrueFalseType",
				basicTypeRegistry,
				"true_false"
		);

		handle(
				YES_NO,
				"org.hibernate.type.YesNoType",
				basicTypeRegistry,
				"yes_no"
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
				WRAPPER_BINARY,
				"org.hibernate.type.WrapperBinaryType",
				basicTypeRegistry,
				"wrapper-binary", "Byte[]", Byte[].class.getName()
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
				WRAPPED_MATERIALIZED_BLOB,
				"org.hibernate.type.MaterializedBlobType",
				basicTypeRegistry,
				"wrapped_materialized_blob"
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
				"wrapper-characters", Character[].class.getName(), "Character[]"
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
				OFFSET_TIME,
				"org.hibernate.type.OffsetTimeType",
				basicTypeRegistry,
				OffsetTime.class.getSimpleName(), OffsetTime.class.getName()
		);

		handle(
				ZONED_DATE_TIME,
				"org.hibernate.type.ZonedDateTimeType",
				basicTypeRegistry,
				ZonedDateTime.class.getSimpleName(), ZonedDateTime.class.getName()
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
				"calendar_date"
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
				UUID_BINARY,
				"org.hibernate.type.UUIDBinaryType",
				basicTypeRegistry,
				"uuid-binary", UUID.class.getName()
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


		// Specialized version handlers

		handle(
				ROW_VERSION,
				null,
				basicTypeRegistry,
				"row_version"
		);

		handle(
				DbTimestampType.INSTANCE,
				null,
				basicTypeRegistry,
				DbTimestampType.INSTANCE.getName()
		);

		handle(
				OBJECT_TYPE,
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

		// todo (6.0) - ? how to handle DbTimestampType?
		//		DbTimestampType was really just a variant of TimestampType with overridden
		//		version (opt lock) support
		//handle( DbTimestampType.INSTANCE, typeConfiguration, basicTypeProducerRegistry, "dbtimestamp" );
		//handle( new AdaptedImmutableType( DbTimestampType.INSTANCE ), typeConfiguration,
		//		basicTypeProducerRegistry, "imm_dbtimestamp" );

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

	@SuppressWarnings("rawtypes")
	private static void handle(
			BasicType type,
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
