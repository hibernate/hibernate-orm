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
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#BIT BIT}.
	 *
	 * @see BooleanType
	 */
	public static final BooleanType BOOLEAN = BooleanType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 *
	 * @see NumericBooleanType
	 */
	public static final NumericBooleanType NUMERIC_BOOLEAN = NumericBooleanType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'T'/'F').
	 *
	 * @see TrueFalseType
	 */
	public static final TrueFalseType TRUE_FALSE = TrueFalseType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'Y'/'N').
	 *
	 * @see YesNoType
	 */
	public static final YesNoType YES_NO = YesNoType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Numeric mappings

	/**
	 * The standard Hibernate type for mapping {@link Byte} to JDBC {@link java.sql.Types#TINYINT TINYINT}.
	 */
	public static final ByteType BYTE = ByteType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Short} to JDBC {@link java.sql.Types#SMALLINT SMALLINT}.
	 *
	 * @see ShortType
	 */
	public static final ShortType SHORT = ShortType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Integer} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 *
	 * @see IntegerType
	 */
	public static final IntegerType INTEGER = IntegerType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Long} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 *
	 * @see LongType
	 */
	public static final LongType LONG = LongType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Float} to JDBC {@link java.sql.Types#FLOAT FLOAT}.
	 *
	 * @see FloatType
	 */
	public static final FloatType FLOAT = FloatType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Double} to JDBC {@link java.sql.Types#DOUBLE DOUBLE}.
	 *
	 * @see DoubleType
	 */
	public static final DoubleType DOUBLE = DoubleType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link BigInteger} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 *
	 * @see BigIntegerType
	 */
	public static final BigIntegerType BIG_INTEGER = BigIntegerType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link BigDecimal} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 *
	 * @see BigDecimalType
	 */
	public static final BigDecimalType BIG_DECIMAL = BigDecimalType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Character mappings

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#CHAR CHAR(1)}.
	 *
	 * @see CharacterType
	 */
	public static final CharacterType CHARACTER = CharacterType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#NCHAR NCHAR(1)}.
	 */
	public static final CharacterNCharType CHARACTER_NCHAR = CharacterNCharType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see StringType
	 */
	public static final StringType STRING = StringType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NVARCHAR NVARCHAR}
	 */
	public static final StringNVarcharType NSTRING = StringNVarcharType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CharArrayType
	 */
	public static final CharArrayType CHAR_ARRAY = CharArrayType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC
	 * {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CharacterArrayType
	 */
	public static final CharacterArrayType CHARACTER_ARRAY = CharacterArrayType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_CLOB}
	 *
	 * @see TextType
	 */
	public static final TextType TEXT = TextType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGNVARCHAR LONGNVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_NCLOB}
	 *
	 * @see NTextType
	 */
	public static final NTextType NTEXT = NTextType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Clob} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see ClobType
	 * @see #MATERIALIZED_CLOB
	 */
	public static final ClobType CLOB = ClobType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link NClob} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see NClobType
	 * @see #MATERIALIZED_NCLOB
	 */
	public static final NClobType NCLOB = NClobType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see MaterializedClobType
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final MaterializedClobType MATERIALIZED_CLOB = MaterializedClobType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see MaterializedNClobType
	 * @see #MATERIALIZED_CLOB
	 * @see #NTEXT
	 */
	public static final MaterializedNClobType MATERIALIZED_NCLOB = MaterializedNClobType.INSTANCE;


	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final CharacterArrayClobType MATERIALIZED_CLOB_CHAR_ARRAY = PrimitiveCharacterArrayClobType.INSTANCE;


	/**
	 * The standard Hibernate type for mapping {@code Character[]} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final CharacterArrayClobType MATERIALIZED_CLOB_CHARACTER_ARRAY = CharacterArrayClobType.INSTANCE;


	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see #MATERIALIZED_NCLOB
	 * @see #TEXT
	 */
	public static final CharacterArrayNClobType MATERIALIZED_NCLOB_CHAR_ARRAY = PrimitiveCharacterArrayNClobType.INSTANCE;


	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC {@link java.sql.Types#NCLOB NCLOB} and
	 *
	 * @see #NCLOB
	 * @see #CHAR_ARRAY
	 */
	public static final CharacterArrayNClobType MATERIALIZED_NCLOB_CHARACTER_ARRAY = CharacterArrayNClobType.INSTANCE;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Date / time data

	/**
	 * The standard Hibernate type for mapping {@link Duration} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 */
	public static final DurationType DURATION = DurationType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link LocalDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final LocalDateTimeType LOCAL_DATE_TIME = LocalDateTimeType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link LocalDate} to JDBC {@link java.sql.Types#DATE DATE}.
	 */
	public static final LocalDateType LOCAL_DATE = LocalDateType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link LocalTime} to JDBC {@link java.sql.Types#TIME TIME}.
	 */
	public static final LocalTimeType LOCAL_TIME = LocalTimeType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link OffsetDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final OffsetDateTimeType OFFSET_DATE_TIME = OffsetDateTimeType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link OffsetTime} to JDBC {@link java.sql.Types#TIME TIME}.
	 */
	public static final OffsetTimeType OFFSET_TIME = OffsetTimeType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link ZonedDateTime} to JDBC {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 */
	public static final ZonedDateTimeType ZONED_DATE_TIME = ZonedDateTimeType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Instant} to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 *
	 * @see TimeType
	 */
	public static final InstantType INSTANT = InstantType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Time}) to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 *
	 * @see TimeType
	 */
	public static final TimeType TIME = TimeType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Date}) to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 *
	 * @see TimeType
	 */
	public static final DateType DATE = DateType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Date} ({@link java.sql.Timestamp}) to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 *
	 * @see TimeType
	 */
	public static final TimestampType TIMESTAMP = TimestampType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 *
	 * @see CalendarType
	 */
	public static final CalendarType CALENDAR = CalendarType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 *
	 * @see CalendarDateType
	 */
	public static final CalendarDateType CALENDAR_DATE = CalendarDateType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Calendar} to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 */
	public static final CalendarTimeType CALENDAR_TIME = CalendarTimeType.INSTANCE;



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Binary mappings

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 *
	 * @see BinaryType
	 */
	public static final BinaryType BINARY = BinaryType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Byte Byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 *
	 * @see WrapperBinaryType
	 */
	public static final WrapperBinaryType WRAPPER_BINARY = WrapperBinaryType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY}.
	 *
	 * @see ImageType
	 * @see #MATERIALIZED_BLOB
	 */
	public static final ImageType IMAGE = ImageType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Blob} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see BlobType
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BlobType BLOB = BlobType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see MaterializedBlobType
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final MaterializedBlobType MATERIALIZED_BLOB = MaterializedBlobType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Serializable} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 * <p/>
	 * See especially the discussion wrt {@link ClassLoader} determination on {@link SerializableType}
	 *
	 * @see SerializableType
	 */
	@SuppressWarnings("rawtypes")
	public static final SerializableType SERIALIZABLE = SerializableType.INSTANCE;

	public static final JavaObjectType OBJECT_TYPE = JavaObjectType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc JDK types

	/**
	 * The standard Hibernate type for mapping {@link Class} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see ClassType
	 */
	public static final ClassType CLASS = ClassType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Locale} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see LocaleType
	 */
	public static final LocaleType LOCALE = LocaleType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Currency} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CurrencyType
	 */
	public static final CurrencyType CURRENCY = CurrencyType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link ZoneOffset} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see ZoneOffsetType
	 */
	public static final ZoneOffsetType ZONE_OFFSET = ZoneOffsetType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link TimeZone} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see TimeZoneType
	 */
	public static final TimeZoneType TIMEZONE = TimeZoneType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.net.URL} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see UrlType
	 */
	public static final UrlType URL = UrlType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UUID mappings

	/**
	 * The standard Hibernate type for mapping {@link UUID} to JDBC {@link java.sql.Types#BINARY BINARY}.
	 *
	 * @see UUIDBinaryType
	 */
	public static final UUIDBinaryType UUID_BINARY = UUIDBinaryType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link UUID} to JDBC {@link java.sql.Types#CHAR CHAR}.
	 *
	 * @see UUIDCharType
	 */
	public static final UUIDCharType UUID_CHAR = UUIDCharType.INSTANCE;


	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY},
	 * specifically for entity versions/timestamps.  Only useful for T-SQL databases (MS, Sybase, etc)
	 *
	 * @see RowVersionType
	 */
	public static final RowVersionType ROW_VERSION = RowVersionType.INSTANCE;


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
				WrappedMaterializedBlobType.INSTANCE,
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

		// todo (6.0) - ? how to handle DbTimestampType?
		//		DbTimestampType was really just a variant of TimestampType with overridden
		//		version (opt lock) support
		//handle( DbTimestampType.INSTANCE, typeConfiguration, basicTypeProducerRegistry, "dbtimestamp" );
		//handle( new AdaptedImmutableType( DbTimestampType.INSTANCE ), typeConfiguration,
		//		basicTypeProducerRegistry, "imm_dbtimestamp" );

		final AdaptedImmutableType dateTypeImmutableType = new AdaptedImmutableType( DateType.INSTANCE );
		handle( dateTypeImmutableType, null, basicTypeRegistry, dateTypeImmutableType.getName() );

		final AdaptedImmutableType timeTypeImmutableType = new AdaptedImmutableType( TimeType.INSTANCE );
		handle( timeTypeImmutableType, null, basicTypeRegistry, timeTypeImmutableType.getName() );

		final AdaptedImmutableType timeStampImmutableType = new AdaptedImmutableType( TimestampType.INSTANCE );
		handle( timeStampImmutableType, null, basicTypeRegistry, timeStampImmutableType.getName() );

		final AdaptedImmutableType calendarImmutableType = new AdaptedImmutableType( CalendarType.INSTANCE );
		handle( calendarImmutableType, null, basicTypeRegistry, calendarImmutableType.getName() );

		final AdaptedImmutableType calendarDateImmutableType = new AdaptedImmutableType( CalendarDateType.INSTANCE );
		handle( calendarDateImmutableType, null, basicTypeRegistry, calendarDateImmutableType.getName() );

		//noinspection unchecked
		final AdaptedImmutableType binaryImmutableType = new AdaptedImmutableType( BinaryType.INSTANCE );
		handle( binaryImmutableType, null, basicTypeRegistry, binaryImmutableType.getName() );

		//noinspection unchecked
		final AdaptedImmutableType serializableImmutableType = new AdaptedImmutableType( SerializableType.INSTANCE );
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

}
