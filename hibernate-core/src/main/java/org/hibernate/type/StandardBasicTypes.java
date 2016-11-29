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
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.boot.model.type.spi.BasicTypeProducerRegistry;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.Type;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.basic.RegistryKeyImpl;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Centralizes access to the standard set of basic {@link Type types}.
 * <p/>
 * Type mappings can be adjusted per {@link org.hibernate.SessionFactory}.  These adjusted mappings can be accessed
 * from the {@link org.hibernate.TypeHelper} instance obtained via {@link org.hibernate.SessionFactory#getTypeHelper()}
 *
 * @see BasicTypeRegistry
 * @see org.hibernate.TypeHelper
 * @see org.hibernate.SessionFactory#getTypeHelper()
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
public final class StandardBasicTypes {
	private StandardBasicTypes() {
	}

	private static final Set<SqlTypeDescriptor> SQL_TYPE_DESCRIPTORS = new HashSet<>();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// boolean data

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#BIT BIT}.
	 *
	 * @see BooleanType
	 */
	public static final org.hibernate.type.spi.BasicType<Boolean> BOOLEAN = BooleanType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 *
	 * @see NumericBooleanType
	 */
	public static final org.hibernate.type.spi.BasicType<Boolean> NUMERIC_BOOLEAN = NumericBooleanType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'T'/'F').
	 *
	 * @see TrueFalseType
	 */
	public static final BasicType<Boolean> TRUE_FALSE = TrueFalseType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'Y'/'N').
	 *
	 * @see YesNoType
	 */
	public static final BasicType<Boolean> YES_NO = YesNoType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// byte/binary data

	/**
	 * The standard Hibernate type for mapping {@link Byte} to JDBC {@link java.sql.Types#TINYINT TINYINT}.
	 */
	public static final org.hibernate.type.spi.BasicType<Byte> BYTE = ByteSupport.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 *
	 * @see BinaryType
	 */
	public static final org.hibernate.type.spi.BasicType<byte[]> BINARY = BinaryType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Byte Byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 *
	 * @see WrapperBinaryType
	 */
	public static final org.hibernate.type.spi.BasicType<Byte[]> WRAPPER_BINARY = WrapperBinaryType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY}.
	 *
	 * @see ImageType
	 * @see #MATERIALIZED_BLOB
	 */
	public static final org.hibernate.type.spi.BasicType<byte[]> IMAGE = ImageType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.sql.Blob} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see BlobType
	 * @see #MATERIALIZED_BLOB
	 */
	public static final org.hibernate.type.spi.BasicType<Blob> BLOB = BlobType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see MaterializedBlobType
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final org.hibernate.type.spi.BasicType<byte[]> MATERIALIZED_BLOB = MaterializedBlobType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// numeric data

	/**
	 * The standard Hibernate type for mapping {@link Short} to JDBC {@link java.sql.Types#SMALLINT SMALLINT}.
	 *
	 * @see ShortType
	 */
	public static final org.hibernate.type.spi.BasicType<Short> SHORT = ShortType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Integer} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 *
	 * @see IntegerType
	 */
	public static final org.hibernate.type.spi.BasicType<Integer> INTEGER = IntegerType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Long} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 *
	 * @see LongType
	 */
	public static final org.hibernate.type.spi.BasicType<Long> LONG = LongType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Float} to JDBC {@link java.sql.Types#FLOAT FLOAT}.
	 *
	 * @see FloatType
	 */
	public static final org.hibernate.type.spi.BasicType<Float> FLOAT = FloatType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Double} to JDBC {@link java.sql.Types#DOUBLE DOUBLE}.
	 *
	 * @see DoubleType
	 */
	public static final org.hibernate.type.spi.BasicType<Double> DOUBLE = DoubleType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.math.BigInteger} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 *
	 * @see BigIntegerType
	 */
	public static final org.hibernate.type.spi.BasicType<BigInteger> BIG_INTEGER = BigIntegerType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.math.BigDecimal} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 *
	 * @see BigDecimalType
	 */
	public static final org.hibernate.type.spi.BasicType<BigDecimal> BIG_DECIMAL = BigDecimalType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// String / character data

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#CHAR CHAR(1)}.
	 *
	 * @see CharacterType
	 */
	public static final org.hibernate.type.spi.BasicType<Character> CHARACTER = CharacterType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see StringType
	 */
	public static final org.hibernate.type.spi.BasicType<String> STRING = StringType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CharArrayType
	 */
	public static final org.hibernate.type.spi.BasicType<char[]> CHAR_ARRAY = CharArrayType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC
	 * {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CharacterArrayType
	 */
	public static final org.hibernate.type.spi.BasicType<Character[]> CHARACTER_ARRAY = CharacterArrayType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_CLOB}
	 *
	 * @see TextType
	 */
	public static final org.hibernate.type.spi.BasicType<String> TEXT = TextType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGNVARCHAR LONGNVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_NCLOB}
	 *
	 * @see NTextType
	 */
	public static final org.hibernate.type.spi.BasicType<String> NTEXT = NTextType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.sql.Clob} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see ClobType
	 * @see #MATERIALIZED_CLOB
	 */
	public static final org.hibernate.type.spi.BasicType<Clob> CLOB = ClobType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.sql.NClob} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see NClobType
	 * @see #MATERIALIZED_NCLOB
	 */
	public static final org.hibernate.type.spi.BasicType<NClob> NCLOB = NClobType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see MaterializedClobType
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final org.hibernate.type.spi.BasicType<String> MATERIALIZED_CLOB = MaterializedClobType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see MaterializedNClobType
	 * @see #MATERIALIZED_CLOB
	 * @see #NTEXT
	 */
	public static final org.hibernate.type.spi.BasicType<String> MATERIALIZED_NCLOB = MaterializedNClobType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Date / time data

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Time}) to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 *
	 * @see TimeType
	 */
	public static final org.hibernate.type.spi.BasicType<Date> TIME = TimeType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Date}) to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 *
	 * @see TimeType
	 */
	public static final org.hibernate.type.spi.BasicType<Date> DATE = DateType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Timestamp}) to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 *
	 * @see TimeType
	 */
	public static final org.hibernate.type.spi.BasicType<Date> TIMESTAMP = TimestampType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Calendar} to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 *
	 * @see CalendarType
	 */
	public static final org.hibernate.type.spi.BasicType<Calendar> CALENDAR = CalendarType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Calendar} to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 *
	 * @see CalendarDateType
	 */
	public static final org.hibernate.type.spi.BasicType<Calendar> CALENDAR_DATE = CalendarDateType.INSTANCE;

	public static final org.hibernate.type.spi.BasicType<Duration> DURATION = DurationType.INSTANCE;
	public static final org.hibernate.type.spi.BasicType<LocalDateTime> LOCAL_DATE_TIME = LocalDateTimeType.INSTANCE;
	public static final org.hibernate.type.spi.BasicType<LocalDate> LOCAL_DATE = LocalDateType.INSTANCE;
	public static final org.hibernate.type.spi.BasicType<LocalTime> LOCAL_TIME = LocalTimeType.INSTANCE;
	public static final org.hibernate.type.spi.BasicType<OffsetDateTime> OFFSET_DATE_TIME = OffsetDateTimeType.INSTANCE;
	public static final org.hibernate.type.spi.BasicType<OffsetTime> OFFSET_TIME = OffsetTimeType.INSTANCE;
	public static final org.hibernate.type.spi.BasicType<ZonedDateTime> ZONED_DATE_TIME = ZonedDateTimeType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// UUID data

	/**
	 * The standard Hibernate type for mapping {@link java.util.UUID} to JDBC {@link java.sql.Types#BINARY BINARY}.
	 *
	 * @see UUIDBinaryType
	 */
	public static final org.hibernate.type.spi.BasicType<UUID> UUID_BINARY = UUIDBinaryType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.UUID} to JDBC {@link java.sql.Types#CHAR CHAR}.
	 *
	 * @see UUIDCharType
	 */
	public static final org.hibernate.type.spi.BasicType<UUID> UUID_CHAR = UUIDCharType.INSTANCE;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Misc data

	/**
	 * The standard Hibernate type for mapping {@link Class} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see ClassType
	 */
	public static final org.hibernate.type.spi.BasicType<Class> CLASS = ClassType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Currency} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CurrencyType
	 */
	public static final org.hibernate.type.spi.BasicType<Currency> CURRENCY = CurrencyType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Locale} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see LocaleType
	 */
	public static final org.hibernate.type.spi.BasicType<Locale> LOCALE = LocaleType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.io.Serializable} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 * <p/>
	 * See especially the discussion wrt {@link ClassLoader} determination on {@link SerializableType}
	 *
	 * @see SerializableType
	 */
	public static final org.hibernate.type.spi.BasicType<Serializable> SERIALIZABLE = SerializableType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.TimeZone} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see TimeZoneType
	 */
	public static final org.hibernate.type.spi.BasicType<TimeZone> TIMEZONE = TimeZoneType.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.net.URL} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see UrlType
	 */
	public static final org.hibernate.type.spi.BasicType<java.net.URL> URL = UrlType.INSTANCE;


	public static void prime(TypeConfiguration typeConfiguration, BasicTypeProducerRegistry basicTypeProducerRegistry) {
		// boolean data
		handle( BOOLEAN, typeConfiguration, basicTypeProducerRegistry, "boolean", boolean.class.getName(), Boolean.class.getName() );
		handle( NUMERIC_BOOLEAN, typeConfiguration, basicTypeProducerRegistry, "numeric_boolean" );
		handle( TRUE_FALSE, typeConfiguration, basicTypeProducerRegistry, "true_false" );
		handle( YES_NO, typeConfiguration, basicTypeProducerRegistry, "yes_no" );

		// byte/binary data
		handle( BYTE, typeConfiguration, basicTypeProducerRegistry, "byte", byte.class.getName(), Byte.class.getName() );
		handle( BINARY, typeConfiguration, basicTypeProducerRegistry, "binary", "byte[]", byte[].class.getName() );
		handle( WRAPPER_BINARY, typeConfiguration, basicTypeProducerRegistry, "wrapper-binary", "Byte[]", Byte[].class.getName() );
		handle( IMAGE, typeConfiguration, basicTypeProducerRegistry, "image" );
		handle( BLOB, typeConfiguration, basicTypeProducerRegistry, "blob", Blob.class.getName() );
		handle( MATERIALIZED_BLOB, typeConfiguration, basicTypeProducerRegistry, "materialized_blob" );

		// Numeric data
		handle( SHORT, typeConfiguration, basicTypeProducerRegistry, "short", short.class.getName(), Short.class.getName() );
		handle( INTEGER, typeConfiguration, basicTypeProducerRegistry, "integer", int.class.getName(), Integer.class.getName() );
		handle( LONG, typeConfiguration, basicTypeProducerRegistry, "long", long.class.getName(), Long.class.getName() );
		handle( FLOAT, typeConfiguration, basicTypeProducerRegistry, "float", float.class.getName(), Float.class.getName() );
		handle( DOUBLE, typeConfiguration, basicTypeProducerRegistry, "double", double.class.getName(), Double.class.getName() );
		handle( BIG_INTEGER, typeConfiguration, basicTypeProducerRegistry, "big_integer", BigInteger.class.getName() );
		handle( BIG_DECIMAL, typeConfiguration, basicTypeProducerRegistry, "big_decimal", BigDecimal.class.getName() );

		// String data
		handle( CHARACTER, typeConfiguration, basicTypeProducerRegistry, "character", char.class.getName(), Character.class.getName() );
		handle( STRING, typeConfiguration, basicTypeProducerRegistry, "string", String.class.getName() );
		handle( CHAR_ARRAY, typeConfiguration, basicTypeProducerRegistry, "characters", "char[]", char[].class.getName() );
		handle( CHARACTER_ARRAY, typeConfiguration,
				basicTypeProducerRegistry, "wrapper-characters", Character[].class.getName(), "Character[]" );
		handle( TEXT, typeConfiguration, basicTypeProducerRegistry, "text" );
		handle( NTEXT, typeConfiguration, basicTypeProducerRegistry, "ntext" );
		handle( CLOB, typeConfiguration, basicTypeProducerRegistry, "clob", Clob.class.getName() );
		handle( NCLOB, typeConfiguration, basicTypeProducerRegistry, "nclob", NClob.class.getName() );
		handle( MATERIALIZED_CLOB, typeConfiguration, basicTypeProducerRegistry, "materialized_clob" );
		handle( MATERIALIZED_NCLOB, typeConfiguration, basicTypeProducerRegistry, "materialized_nclob" );

		// date / time data
		handle( DATE, typeConfiguration, basicTypeProducerRegistry, "date", java.sql.Date.class.getName() );
		handle( TIME, typeConfiguration, basicTypeProducerRegistry, "time", java.sql.Time.class.getName() );
		handle( TIMESTAMP, typeConfiguration,
				basicTypeProducerRegistry, "timestamp", java.sql.Timestamp.class.getName(), java.util.Date.class.getName() );
		handle( CALENDAR, typeConfiguration,
				basicTypeProducerRegistry, "calendar", Calendar.class.getName(), GregorianCalendar.class.getName() );
		handle( CALENDAR_DATE, typeConfiguration, basicTypeProducerRegistry, "calendar_date" );
		handle( DURATION, typeConfiguration, basicTypeProducerRegistry, Duration.class.getSimpleName(), Duration.class.getName() );
		handle( LOCAL_DATE_TIME, typeConfiguration,
				basicTypeProducerRegistry, LocalDateTime.class.getSimpleName(), LocalDateTime.class.getName() );
		handle( LOCAL_DATE, typeConfiguration, basicTypeProducerRegistry, LocalDate.class.getSimpleName(), LocalDate.class.getName() );
		handle( LOCAL_TIME, typeConfiguration, basicTypeProducerRegistry, LocalTime.class.getSimpleName(), LocalTime.class.getName() );
		handle( OFFSET_DATE_TIME, typeConfiguration,
				basicTypeProducerRegistry, OffsetDateTime.class.getSimpleName(), OffsetDateTime.class.getName() );
		handle( OFFSET_TIME, typeConfiguration,
				basicTypeProducerRegistry, OffsetTime.class.getSimpleName(), OffsetTime.class.getName() );
		handle( ZONED_DATE_TIME, typeConfiguration,
				basicTypeProducerRegistry, ZonedDateTime.class.getSimpleName(), ZonedDateTime.class.getName() );
		handle( DbTimestampType.INSTANCE, typeConfiguration, basicTypeProducerRegistry, "dbtimestamp" );

		// UUID data
		handle( UUID_BINARY, typeConfiguration, basicTypeProducerRegistry, "uuid-binary", UUID.class.getName() );
		handle( UUID_CHAR, typeConfiguration, basicTypeProducerRegistry, "uuid-char" );

		// Misc data
		handle( CLASS, typeConfiguration, basicTypeProducerRegistry, "class", Class.class.getName() );
		handle( CURRENCY, typeConfiguration,
				basicTypeProducerRegistry, "currency", Currency.class.getSimpleName(), Currency.class.getName() );
		handle( LOCALE, typeConfiguration, basicTypeProducerRegistry, "locale", Locale.class.getName() );
		handle( SERIALIZABLE, typeConfiguration, basicTypeProducerRegistry, "serializable", Serializable.class.getName() );
		handle( TIMEZONE, typeConfiguration, basicTypeProducerRegistry, "timezone", TimeZone.class.getName() );
		handle( URL, typeConfiguration, basicTypeProducerRegistry, "url", java.net.URL.class.getName() );

		// immutable variants of certain types
		handle( new AdaptedImmutableType( DateType.INSTANCE ), typeConfiguration,
				basicTypeProducerRegistry, "imm_date" );
		handle( new AdaptedImmutableType( TimeType.INSTANCE ), typeConfiguration,
				basicTypeProducerRegistry, "imm_time" );
		handle( new AdaptedImmutableType( TimestampType.INSTANCE ), typeConfiguration,
				basicTypeProducerRegistry, "imm_timestamp" );
		handle( new AdaptedImmutableType( DbTimestampType.INSTANCE ), typeConfiguration,
				basicTypeProducerRegistry, "imm_dbtimestamp" );
		handle( new AdaptedImmutableType( CalendarType.INSTANCE ), typeConfiguration,
				basicTypeProducerRegistry, "imm_calendar" );
		handle( new AdaptedImmutableType( CalendarDateType.INSTANCE ), typeConfiguration,
				basicTypeProducerRegistry, "imm_calendar_date" );
		handle( new AdaptedImmutableType( BinaryType.INSTANCE ), typeConfiguration,
				basicTypeProducerRegistry, "imm_binary" );
		handle( new AdaptedImmutableType( SerializableType.INSTANCE ), typeConfiguration,
				basicTypeProducerRegistry, "imm_serializable" );
	}

	private static void handle(
			BasicType basicType,
			TypeConfiguration typeConfiguration,
			BasicTypeProducerRegistry basicTypeProducerRegistry,
			String... registrationKeys) {
		typeConfiguration.getBasicTypeRegistry().register(
				basicType,
				RegistryKeyImpl.from( basicType )
		);

		basicTypeProducerRegistry.register(
				basicType,
				BasicTypeProducerRegistry.DuplicationStrategy.OVERWRITE,
				basicType.getClass().getName()
		);

		basicTypeProducerRegistry.register(
				basicType,
				BasicTypeProducerRegistry.DuplicationStrategy.OVERWRITE,
				registrationKeys
		);
	}
}
