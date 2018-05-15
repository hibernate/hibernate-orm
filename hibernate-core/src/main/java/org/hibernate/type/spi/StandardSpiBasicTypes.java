/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.descriptor.java.internal.BigDecimalJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.BigIntegerJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.BlobJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.BooleanJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ByteArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ByteJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CalendarDateJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CalendarJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CalendarTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CharacterArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CharacterJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ClassJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ClobJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CurrencyJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.DateJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.DoubleJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.DurationJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.FloatJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.InstantJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LocalDateJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LocalDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LocalTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LocaleJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LongJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.NClobJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.OffsetTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.PrimitiveByteArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.PrimitiveCharacterArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.SerializableJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ShortJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.StringJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.TimeZoneJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.UUIDJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.UrlJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ZonedDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.BigIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BinarySqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BitSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.BlobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.CharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.DateSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.DoubleSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.FloatSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.LongNVarcharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.LongVarbinarySqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.LongVarcharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NCharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NClobSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NVarcharSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.NumericSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SmallIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.TimeSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.TimestampSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.TinyIntSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarbinarySqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.BooleanBasicTypeImpl;

/**
 * Centralizes access to the standard set of basic {@link BasicType types}.
 * <p/>
 * Type mappings can be adjusted per {@link org.hibernate.SessionFactory} (technically per
 * {@link TypeConfiguration}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings({"UnusedDeclaration"})
public final class StandardSpiBasicTypes {
	private StandardSpiBasicTypes() {
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// boolean data

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#BIT BIT}.
	 */
	public static final BasicType<Boolean> BOOLEAN = new BasicTypeImpl<>(
			BooleanJavaDescriptor.INSTANCE,
			BitSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 */
	public static final BasicType<Boolean> NUMERIC_BOOLEAN = new BooleanBasicTypeImpl<>(
			BooleanJavaDescriptor.INSTANCE,
			NumericSqlDescriptor.INSTANCE,
			1,
			0
	);

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'T'/'F').
	 */
	public static final BasicType<Boolean> TRUE_FALSE = new BooleanBasicTypeImpl<>(
			BooleanJavaDescriptor.INSTANCE,
			CharSqlDescriptor.INSTANCE,
			'T',
			'F'
	);

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'Y'/'N').
	 */
	public static final BasicType<Boolean> YES_NO = new BooleanBasicTypeImpl<>(
			BooleanJavaDescriptor.INSTANCE,
			CharSqlDescriptor.INSTANCE,
			'Y',
			'N'
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// byte/binary data

	/**
	 * The standard Hibernate type for mapping {@link Byte} to JDBC {@link java.sql.Types#TINYINT TINYINT}.
	 */
	public static final BasicType<Byte> BYTE = new BasicTypeImpl<>(
			ByteJavaDescriptor.INSTANCE,
			TinyIntSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 */
	public static final BasicType<byte[]> BINARY = new BasicTypeImpl<>(
			PrimitiveByteArrayJavaDescriptor.INSTANCE,
			VarbinarySqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Byte Byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 */
	public static final BasicType<Byte[]> WRAPPER_BINARY = new BasicTypeImpl(
			ByteArrayJavaDescriptor.INSTANCE,
			VarbinarySqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY}.
	 *
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BasicType<byte[]> IMAGE = new BasicTypeImpl(
			PrimitiveByteArrayJavaDescriptor.INSTANCE,
			LongVarbinarySqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Blob} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BasicType<Blob> BLOB = new BasicTypeImpl(
			BlobJavaDescriptor.INSTANCE,
			BlobSqlDescriptor.DEFAULT
	);

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BasicType<byte[]> MATERIALIZED_BLOB = new BasicTypeImpl(
			PrimitiveByteArrayJavaDescriptor.INSTANCE,
			BlobSqlDescriptor.DEFAULT
	);

	/**
	 * The standard Hibernate type for mapping {@code Byte[]} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final BasicType<byte[]> WRAPPED_MATERIALIZED_BLOB = new BasicTypeImpl(
			ByteArrayJavaDescriptor.INSTANCE,
			BlobSqlDescriptor.DEFAULT
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// numeric data

	/**
	 * The standard Hibernate type for mapping {@link Short} to JDBC {@link java.sql.Types#SMALLINT SMALLINT}.
	 */
	public static final BasicType<Short> SHORT = new BasicTypeImpl(
			ShortJavaDescriptor.INSTANCE,
			SmallIntSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Integer} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 */
	public static final BasicType<Integer> INTEGER = new BasicTypeImpl(
			IntegerJavaDescriptor.INSTANCE,
			IntegerSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Long} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 */
	public static final BasicType<Long> LONG = new BasicTypeImpl(
			LongJavaDescriptor.INSTANCE,
			BigIntSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Float} to JDBC {@link java.sql.Types#FLOAT FLOAT}.
	 */
	public static final BasicType<Float> FLOAT = new BasicTypeImpl(
			FloatJavaDescriptor.INSTANCE,
			FloatSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Double} to JDBC {@link java.sql.Types#DOUBLE DOUBLE}.
	 */
	public static final BasicType<Double> DOUBLE = new BasicTypeImpl(
			DoubleJavaDescriptor.INSTANCE,
			DoubleSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link BigInteger} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 */
	public static final BasicType<BigInteger> BIG_INTEGER = new BasicTypeImpl(
			BigIntegerJavaDescriptor.INSTANCE,
			NumericSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link BigDecimal} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 */
	public static final BasicType<BigDecimal> BIG_DECIMAL = new BasicTypeImpl(
			BigDecimalJavaDescriptor.INSTANCE,
			NumericSqlDescriptor.INSTANCE
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// String / character data

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#CHAR CHAR(1)}.
	 */
	public static final BasicType<Character> CHARACTER = new BasicTypeImpl(
			CharacterJavaDescriptor.INSTANCE,
			CharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#NCHAR NCHAR(1)}.
	 */
	public static final BasicType<Character> CHARACTER_NCHAR = new BasicTypeImpl(
			CharacterJavaDescriptor.INSTANCE,
			NCharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicType<String> STRING = new BasicTypeImpl(
			StringJavaDescriptor.INSTANCE,
			VarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NVARCHAR NVARCHAR}.
	 */
	public static final BasicType<String> NSTRING = new BasicTypeImpl(
			StringJavaDescriptor.INSTANCE,
			NVarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicType<char[]> CHAR_ARRAY = new BasicTypeImpl(
			PrimitiveCharacterArrayJavaDescriptor.INSTANCE,
			VarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC
	 * {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicType<Character[]> CHARACTER_ARRAY = new BasicTypeImpl(
			CharacterArrayJavaDescriptor.INSTANCE,
			VarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_CLOB}
	 */
	public static final BasicType<String> TEXT = new BasicTypeImpl(
			StringJavaDescriptor.INSTANCE,
			LongVarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGNVARCHAR LONGNVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_NCLOB}
	 */
	public static final BasicType<String> NTEXT = new BasicTypeImpl(
			StringJavaDescriptor.INSTANCE,
			LongNVarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Clob} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 */
	public static final BasicType<Clob> CLOB = new BasicTypeImpl(
			ClobJavaDescriptor.INSTANCE,
			ClobSqlDescriptor.CLOB_BINDING
	);

	/**
	 * The standard Hibernate type for mapping {@link NClob} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_NCLOB
	 */
	public static final BasicType<NClob> NCLOB = new BasicTypeImpl(
			NClobJavaDescriptor.INSTANCE,
			NClobSqlDescriptor.DEFAULT
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #TEXT
	 */
	public static final BasicType<String> MATERIALIZED_CLOB = new BasicTypeImpl(
			StringJavaDescriptor.INSTANCE,
			ClobSqlDescriptor.DEFAULT
	);

	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC {@link java.sql.Types#NCLOB NCLOB} and
	 *
	 * @see #NCLOB
	 * @see #CHAR_ARRAY
	 */
	public static final BasicType<Character[]> MATERIALIZED_NCLOB_CHARACTER_ARRAY = new BasicTypeImpl(
			CharacterArrayJavaDescriptor.INSTANCE,
			NClobSqlDescriptor.DEFAULT
	);

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final BasicType<String> MATERIALIZED_CLOB_CHAR_ARRAY = new BasicTypeImpl(
			PrimitiveCharacterArrayJavaDescriptor.INSTANCE,
			ClobSqlDescriptor.DEFAULT
	);

	/**
	 * The standard Hibernate type for mapping {@code Character[]} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final BasicType<String> MATERIALIZED_CLOB_CHARACTER_ARRAY = new BasicTypeImpl(
			CharacterArrayJavaDescriptor.INSTANCE,
			ClobSqlDescriptor.DEFAULT
	);

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_NCLOB
	 * @see #TEXT
	 */
	public static final BasicType<String> MATERIALIZED_NCLOB_CHAR_ARRAY = new BasicTypeImpl(
			PrimitiveCharacterArrayJavaDescriptor.INSTANCE,
			NClobSqlDescriptor.DEFAULT
	);

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #NTEXT
	 */
	public static final BasicType<String> MATERIALIZED_NCLOB = new BasicTypeImpl(
			StringJavaDescriptor.INSTANCE,
			NClobSqlDescriptor.DEFAULT
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Date / time data

	/**
	 * The standard Hibernate type for mapping {@link Duration} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 */
	public static final BasicType<Duration> DURATION = new BasicTypeImpl(
			DurationJavaDescriptor.INSTANCE,
			BigIntSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link LocalDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicType<LocalDateTime> LOCAL_DATE_TIME = new BasicTypeImpl<>(
			LocalDateTimeJavaDescriptor.INSTANCE,
			TimestampSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link LocalDate} to JDBC {@link java.sql.Types#DATE DATE}.
	 */
	public static final BasicType<LocalDate> LOCAL_DATE = new BasicTypeImpl(
			LocalDateJavaDescriptor.INSTANCE,
			DateSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link LocalTime} to JDBC {@link java.sql.Types#TIME TIME}.
	 */
	public static final BasicType<LocalTime> LOCAL_TIME = new BasicTypeImpl(
			LocalTimeJavaDescriptor.INSTANCE,
			TimeSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicType<OffsetDateTime> OFFSET_DATE_TIME = new BasicTypeImpl(
			OffsetDateTimeJavaDescriptor.INSTANCE,
			TimestampSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link OffsetTime} to JDBC {@link java.sql.Types#TIME TIME}.
	 */
	public static final BasicType<OffsetTime> OFFSET_TIME = new BasicTypeImpl(
			OffsetTimeJavaDescriptor.INSTANCE,
			TimeSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link ZonedDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicType<ZonedDateTime> ZONED_DATE_TIME = new BasicTypeImpl(
			ZonedDateTimeJavaDescriptor.INSTANCE,
			TimestampSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Time}) to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 */
	public static final BasicType<Date> TIME = new BasicTypeImpl(
			DateJavaDescriptor.INSTANCE,
			TimeSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Date}) to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 */
	public static final BasicType<Date> DATE = new BasicTypeImpl(
			DateJavaDescriptor.INSTANCE,
			DateSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Timestamp}) to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicType<Date> TIMESTAMP = new BasicTypeImpl(
			DateJavaDescriptor.INSTANCE,
			TimestampSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicType<Calendar> CALENDAR = new BasicTypeImpl(
			CalendarJavaDescriptor.INSTANCE,
			TimestampSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 */
	public static final BasicType<Calendar> CALENDAR_DATE = new BasicTypeImpl(
			CalendarDateJavaDescriptor.INSTANCE,
			DateSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 */
	public static final BasicType<Calendar> CALENDAR_TIME = new BasicTypeImpl(
			CalendarTimeJavaDescriptor.INSTANCE,
			TimeSqlDescriptor.INSTANCE
	);


	/**
	 * The standard Hibernate type for mapping {@link Instant} to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final BasicType<Instant> INSTANT = new BasicTypeImpl(
			InstantJavaDescriptor.INSTANCE, TimestampSqlDescriptor.INSTANCE
	);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UUID data

	/**
	 * The standard Hibernate type for mapping {@link UUID} to JDBC {@link java.sql.Types#BINARY BINARY}.
	 */
	public static final BasicType<UUID> UUID_BINARY = new BasicTypeImpl(
			UUIDJavaDescriptor.INSTANCE,
			BinarySqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link UUID} to JDBC {@link java.sql.Types#CHAR CHAR}.
	 */
	public static final BasicType<UUID> UUID_CHAR = new BasicTypeImpl(
			UUIDJavaDescriptor.INSTANCE,
			CharSqlDescriptor.INSTANCE
	);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc data

	/**
	 * The standard Hibernate type for mapping {@link Class} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicType<Class> CLASS = new BasicTypeImpl(
			ClassJavaDescriptor.INSTANCE,
			VarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Currency} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicType<Currency> CURRENCY = new BasicTypeImpl(
			CurrencyJavaDescriptor.INSTANCE,
			VarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Locale} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicType<Locale> LOCALE = new BasicTypeImpl(
			LocaleJavaDescriptor.INSTANCE,
			VarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link Serializable} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 */
	public static final BasicType<Serializable> SERIALIZABLE = new BasicTypeImpl(
			SerializableJavaDescriptor.INSTANCE,
			VarbinarySqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link TimeZone} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicType<TimeZone> TIMEZONE = new BasicTypeImpl(
			TimeZoneJavaDescriptor.INSTANCE,
			VarcharSqlDescriptor.INSTANCE
	);

	/**
	 * The standard Hibernate type for mapping {@link java.net.URL} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 */
	public static final BasicType<java.net.URL> URL = new BasicTypeImpl(
			UrlJavaDescriptor.INSTANCE,
			VarcharSqlDescriptor.INSTANCE
	);


	public static void prime(TypeConfiguration typeConfiguration) {

		// todo (6.0) : possibly use this as an opportunity to register cast-target names (HQL,JPQL)

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// boolean data

		handle(
				BOOLEAN,
				"org.hibernate.type.BooleanType",
				typeConfiguration,
				"boolean", boolean.class.getName(), Boolean.class.getName()
		);

		handle(
				NUMERIC_BOOLEAN,
				"org.hibernate.type.NumericBooleanType",
				typeConfiguration,
				"numeric_boolean"
		);

		handle(
				TRUE_FALSE,
				"org.hibernate.type.TrueFalseType",
				typeConfiguration,
				"true_false"
		);

		handle(
				YES_NO,
				"org.hibernate.type.YesNoType",
				typeConfiguration,
				"yes_no"
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// byte/binary data

		handle(
				BYTE,
				"org.hibernate.type.ByteType",
				typeConfiguration,
				"byte", byte.class.getName(), Byte.class.getName()
		);

		handle(
				BINARY,
				"org.hibernate.type.BinaryType",
				typeConfiguration,
				"binary", "byte[]", byte[].class.getName()
		);

		handle(
				WRAPPER_BINARY,
				"org.hibernate.type.WrapperBinaryType",
				typeConfiguration,
				"wrapper-binary", "Byte[]", Byte[].class.getName()
		);

		handle(
				IMAGE,
				"org.hibernate.type.ImageType",
				typeConfiguration,
				"image"
		);

		handle(
				BLOB,
				"org.hibernate.type.BlobType",
				typeConfiguration,
				"blob",
				Blob.class.getName()
		);

		handle(
				MATERIALIZED_BLOB,
				"org.hibernate.type.MaterializedBlobType",
				typeConfiguration,
				"materialized_blob"
		);

		handle(
				WRAPPED_MATERIALIZED_BLOB,
				"org.hibernate.type.MaterializedBlobType",
				typeConfiguration,
				"wrapped_materialized_blob"
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Numeric data

		handle(
				SHORT,
				"org.hibernate.type.ShortType",
				typeConfiguration,
				"short", short.class.getName(), Short.class.getName()
		);

		handle(
				INTEGER,
				"org.hibernate.type.IntegerType",
				typeConfiguration,
				"integer", int.class.getName(), Integer.class.getName()
		);

		handle(
				LONG,
				"org.hibernate.type.LongType",
				typeConfiguration,
				"long", long.class.getName(), Long.class.getName()
		);

		handle(
				FLOAT,
				"org.hibernate.type.FloatType",
				typeConfiguration,
				"float", float.class.getName(), Float.class.getName()
		);

		handle(
				DOUBLE,
				"org.hibernate.type.DoubleType",
				typeConfiguration,
				"double", double.class.getName(), Double.class.getName()
		);

		handle(
				BIG_INTEGER,
				"org.hibernate.type.BigIntegerType",
				typeConfiguration,
				"big_integer", BigInteger.class.getName()
		);

		handle(
				BIG_DECIMAL,
				"org.hibernate.type.BigDecimalType",
				typeConfiguration,
				"big_decimal", BigDecimal.class.getName()
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// String data

		handle(
				CHARACTER,
				"org.hibernate.type.CharacterType",
				typeConfiguration,
				"character", char.class.getName(), Character.class.getName()
		);

		handle(
				CHARACTER_NCHAR,
				null,
				typeConfiguration,
				"character_nchar"
		);

		handle(
				STRING,
				"org.hibernate.type.StringType",
				typeConfiguration,
				"string", String.class.getName()
		);

		handle(
				NSTRING,
				"org.hibernate.type.StringNVarcharType",
				typeConfiguration,
				"nstring"
		);

		handle(
				CHAR_ARRAY,
				"org.hibernate.type.CharArrayType",
				typeConfiguration,
				"characters", "char[]", char[].class.getName()
		);

		handle(
				CHARACTER_ARRAY,
				"org.hibernate.type.CharacterArrayType",
				typeConfiguration,
				"wrapper-characters", Character[].class.getName(), "Character[]"
		);

		handle(
				TEXT,
				"org.hibernate.type.TextType",
				typeConfiguration,
				"text"
		);

		handle(
				NTEXT,
				"org.hibernate.type.NTextType",
				typeConfiguration,
				"ntext"
		);

		handle(
				CLOB,
				"org.hibernate.type.ClobType",
				typeConfiguration,
				"clob", Clob.class.getName()
		);

		handle(
				NCLOB,
				"org.hibernate.type.NClobType",
				typeConfiguration,
				"nclob", NClob.class.getName()
		);

		handle(
				MATERIALIZED_CLOB,
				"org.hibernate.type.MaterializedClobType",
				typeConfiguration,
				"materialized_clob"
		);

		handle(
				MATERIALIZED_CLOB_CHAR_ARRAY,
				"org.hibernate.type.PrimitiveCharacterArrayClobType",
				typeConfiguration,
				"materialized_clob_char_array"
		);

		handle(
				MATERIALIZED_CLOB_CHARACTER_ARRAY,
				"org.hibernate.type.CharacterArrayClobType",
				typeConfiguration,
				"materialized_clob_character_array"
		);

		handle(
				MATERIALIZED_NCLOB,
				"org.hibernate.type.MaterializedNClobType",
				typeConfiguration,
				"materialized_nclob"
		);

		handle(
				MATERIALIZED_NCLOB_CHARACTER_ARRAY,
				"org.hibernate.type.CharacterArrayNClobType",
				typeConfiguration,
				"materialized_nclob_character_array"
		);

		handle(
				MATERIALIZED_NCLOB_CHAR_ARRAY,
				"org.hibernate.type.PrimitiveCharacterArrayNClobType",
				typeConfiguration,
				"materialized_nclob_char_array"
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// date / time data

		handle(
				DURATION,
				"org.hibernate.type.DurationType",
				typeConfiguration,
				Duration.class.getSimpleName(), Duration.class.getName()
		);

		handle(
				LOCAL_DATE_TIME,
				"org.hibernate.type.LocalDateTimeType",
				typeConfiguration,
				LocalDateTime.class.getSimpleName(), LocalDateTime.class.getName()
		);

		handle(
				LOCAL_DATE,
				"org.hibernate.type.LocalDateType",
				typeConfiguration,
				LocalDate.class.getSimpleName(), LocalDate.class.getName()
		);

		handle(
				LOCAL_TIME,
				"org.hibernate.type.LocalTimeType",
				typeConfiguration,
				LocalTime.class.getSimpleName(), LocalTime.class.getName()
		);

		handle(
				OFFSET_DATE_TIME,
				"org.hibernate.type.OffsetDateTimeType",
				typeConfiguration,
				OffsetDateTime.class.getSimpleName(), OffsetDateTime.class.getName()
		);

		handle(
				OFFSET_TIME,
				"org.hibernate.type.OffsetTimeType",
				typeConfiguration,
				OffsetTime.class.getSimpleName(), OffsetTime.class.getName()
		);

		handle(
				ZONED_DATE_TIME,
				"org.hibernate.type.ZonedDateTimeType",
				typeConfiguration,
				ZonedDateTime.class.getSimpleName(), ZonedDateTime.class.getName()
		);

		handle(
				DATE,
				"org.hibernate.type.DateType",
				typeConfiguration,
				"date", java.sql.Date.class.getName()
		);

		handle(
				TIME,
				"org.hibernate.type.TimeType",
				typeConfiguration,
				"time", java.sql.Time.class.getName()
		);

		handle(
				TIMESTAMP,
				"org.hibernate.type.TimestampType",
				typeConfiguration,
				"timestamp", java.sql.Timestamp.class.getName(), Date.class.getName()
		);

		handle(
				CALENDAR,
				"org.hibernate.type.CalendarType",
				typeConfiguration,
				"calendar", Calendar.class.getName(), GregorianCalendar.class.getName()
		);

		handle(
				CALENDAR_DATE,
				"org.hibernate.type.CalendarDateType",
				typeConfiguration,
				"calendar_date"
		);

		handle(
				CALENDAR_TIME,
				"org.hibernate.type.CalendarTimeType",
				typeConfiguration,
				"calendar_date"
		);

		handle(
				INSTANT,
				"org.hibernate.type.InstantType",
				typeConfiguration,
				"instant", Instant.class.getName()
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// UUID data

		handle(
				UUID_BINARY,
				"org.hibernate.type.UUIDBinaryType",
				typeConfiguration,
				"uuid-binary", UUID.class.getName()
		);

		handle(
				UUID_CHAR,
				"org.hibernate.type.UUIDCharType",
				typeConfiguration,
				"uuid-char"
		);


		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Misc data

		handle(
				CLASS,
				"org.hibernate.type.ClassType",
				typeConfiguration,
				"class", Class.class.getName()
		);

		handle(
				CURRENCY,
				"org.hibernate.type.CurrencyType",
				typeConfiguration,
				"currency", Currency.class.getSimpleName(), Currency.class.getName()
		);

		handle(
				LOCALE,
				"org.hibernate.type.LocaleType",
				typeConfiguration,
				"locale",
				Locale.class.getName()
		);

		handle(
				SERIALIZABLE,
				"org.hibernate.type.SerializableType",
				typeConfiguration,
				"serializable", Serializable.class.getName()
		);

		handle(
				TIMEZONE,
				"org.hibernate.type.TimeZoneType",
				typeConfiguration,
				"timezone", TimeZone.class.getName()
		);

		handle(
				URL,
				"org.hibernate.type.UrlType",
				typeConfiguration,
				"url", java.net.URL.class.getName()
		);


		// todo (6.0) - ? how to handle DbTimestampType?
		//		DbTimestampType was really just a variant of TimestampType with overridden
		//		version (opt lock) support
		//handle( DbTimestampType.INSTANCE, typeConfiguration, basicTypeProducerRegistry, "dbtimestamp" );
		//handle( new AdaptedImmutableType( DbTimestampType.INSTANCE ), typeConfiguration,
		//		basicTypeProducerRegistry, "imm_dbtimestamp" );

	}

	private static void handle(
			BasicType type,
			String legacyTypeClassName,
			TypeConfiguration typeConfiguration,
			String... registrationKeys) {

		final BasicTypeRegistry basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();

		// we add these
		if ( StringHelper.isNotEmpty( legacyTypeClassName ) ) {
			basicTypeRegistry.register( type, legacyTypeClassName );
		}

		basicTypeRegistry.register( type, registrationKeys );
	}
}
